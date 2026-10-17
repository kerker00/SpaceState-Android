package net.grafixmafia.spacestate.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.grafixmafia.spacestate.SpaceStateApplication
import net.grafixmafia.spacestate.data.SettingsStore
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.ObservationFreshness
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.SpaceInfo
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus

data class StatusUiState(
    val endpoint: String? = null,
    val refreshSeconds: Int = SettingsStore.DEFAULT_REFRESH_SECONDS,
    val info: SpaceInfo? = null,
    /** Last reported values; displayRooms removes current flags on expiry or error. */
    val rooms: List<MainframeRoom> = emptyList(),
    val spaceTiming: ObservationTiming? = null,
    val roomsTiming: ObservationTiming? = null,
    /** Successful client completion, never the source observation or failed attempt. */
    val lastUpdate: Instant? = null,
    val roomsLastUpdate: Instant? = null,
    val lastAttempt: Instant? = null,
    val roomsLastAttempt: Instant? = null,
    val error: SpaceStateException? = null,
    val roomsError: SpaceStateException? = null,
    val isRefreshing: Boolean = false,
    val evaluatedAt: Instant = Instant.EPOCH,
) {
    val freshness: ObservationFreshness get() = spaceTiming?.freshness(evaluatedAt) ?: ObservationFreshness.UNKNOWN
    val roomsFreshness: ObservationFreshness get() = roomsTiming?.freshness(evaluatedAt) ?: ObservationFreshness.UNKNOWN
    val status: SpaceStatus get() = if (error == null) {
        spaceTiming?.currentStatus(info?.status ?: SpaceStatus.UNKNOWN, evaluatedAt) ?: SpaceStatus.UNKNOWN
    } else SpaceStatus.UNKNOWN
    val roomsCurrent: Boolean get() = roomsError == null && roomsFreshness == ObservationFreshness.CURRENT
    val displayRooms: List<MainframeRoom> get() = if (roomsCurrent) rooms else rooms.map {
        it.copy(state = MainframeRoomState.UNKNOWN, rawState = "")
    }
}

/** Retains source evidence and updates effective states independently of HTTP polling. */
class StatusViewModel(
    private val source: StatusSource,
    private val settings: SettingsStore,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private data class Selection(val endpoint: String, val seconds: Int, val revision: Long)
    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()
    private val selection = MutableStateFlow<Selection?>(null)
    private val fetching = Mutex()
    private var manualJob: Job? = null
    private var expiryJob: Job? = null

    init {
        viewModelScope.launch {
            settings.settings.collect { settings ->
                val previous = selection.value
                val changed = previous?.endpoint != settings.endpoint
                val choice = Selection(settings.endpoint, settings.refreshSeconds, (previous?.revision ?: 0) + if (changed) 1 else 0)
                if (changed) {
                    manualJob?.cancel()
                    expiryJob?.cancel()
                    _state.value = StatusUiState(endpoint = settings.endpoint, refreshSeconds = settings.refreshSeconds, evaluatedAt = now())
                } else _state.update { it.copy(refreshSeconds = settings.refreshSeconds) }
                selection.value = choice
            }
        }
    }

    /** repeatOnLifecycle cancels polling in the background; return evaluates before fetching. */
    suspend fun poll() {
        recheck()
        selection.filterNotNull().collectLatest { choice ->
            while (true) {
                recheck()
                fetch(choice)
                delay(choice.seconds * 1000L)
            }
        }
    }

    fun refresh() {
        val choice = selection.value ?: return
        if (manualJob?.isActive == true) return
        recheck()
        manualJob = viewModelScope.launch {
            if (!current(choice)) return@launch
            _state.update { it.copy(isRefreshing = true) }
            try { fetch(choice) }
            finally { if (current(choice)) _state.update { it.copy(isRefreshing = false) } }
        }
    }

    fun selectSpace(endpoint: String) { viewModelScope.launch { settings.setEndpoint(endpoint) } }
    fun setRefreshSeconds(seconds: Int) { viewModelScope.launch { settings.setRefreshSeconds(seconds) } }
    private fun current(choice: Selection) = selection.value?.revision == choice.revision

    fun recheck() {
        _state.update { it.copy(evaluatedAt = now()) }
        armExpiry()
    }

    private fun armExpiry() {
        expiryJob?.cancel()
        expiryJob = viewModelScope.launch {
            while (true) {
                val date = now()
                _state.update { it.copy(evaluatedAt = date) }
                val expiry = listOfNotNull(_state.value.spaceTiming?.expiresAt, _state.value.roomsTiming?.expiresAt)
                    .filter { it > date }.minOrNull() ?: return@launch
                delay(Duration.between(date, expiry).toMillis().coerceIn(1L, 60_000L))
            }
        }
    }

    private suspend fun fetch(choice: Selection) = fetching.withLock {
        if (!current(choice)) return@withLock
        val attempt = now()
        val hasRooms = source.isMainframe(choice.endpoint)
        _state.update { it.copy(lastAttempt = attempt, roomsLastAttempt = if (hasRooms) attempt else null, evaluatedAt = attempt) }
        coroutineScope {
            launch {
                val result = read { source.spaceObservation(choice.endpoint) }
                currentCoroutineContext().ensureActive()
                if (current(choice)) {
                    val date = now()
                    _state.update { state -> result.fold(
                        onSuccess = { state.copy(info = it.value, spaceTiming = it.timing, error = null, lastUpdate = date, evaluatedAt = date) },
                        onFailure = { state.copy(error = it.classified(), evaluatedAt = date) },
                    ) }
                    armExpiry()
                }
            }
            if (hasRooms) launch {
                val result = read { source.mainframeRoomsObservation() }
                currentCoroutineContext().ensureActive()
                if (current(choice)) {
                    val date = now()
                    _state.update { state -> result.fold(
                        onSuccess = { state.copy(rooms = it.value, roomsTiming = it.timing, roomsError = null, roomsLastUpdate = date, evaluatedAt = date) },
                        onFailure = { state.copy(roomsError = it.classified(), evaluatedAt = date) },
                    ) }
                    armExpiry()
                }
            }
        }
    }

    private suspend fun <T> read(block: suspend () -> T): Result<T> = try { Result.success(block()) }
    catch (error: CancellationException) { throw error }
    catch (error: Exception) { Result.failure(error) }

    companion object {
        val Factory = viewModelFactory { initializer {
            val app = this[APPLICATION_KEY] as SpaceStateApplication
            StatusViewModel(app.statusSource, app.settings)
        } }
    }
}

private fun Throwable.classified(): SpaceStateException = this as? SpaceStateException ?: SpaceStateException.Other(toString())
