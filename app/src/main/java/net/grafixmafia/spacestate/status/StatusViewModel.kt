package net.grafixmafia.spacestate.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.grafixmafia.spacestate.SpaceStateApplication
import net.grafixmafia.spacestate.data.SettingsStore
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.SpaceInfo
import net.grafixmafia.spacestatekit.SpaceStatus

data class StatusUiState(
    /** Null until the settings are loaded. */
    val endpoint: String? = null,
    val refreshSeconds: Int = SettingsStore.DEFAULT_REFRESH_SECONDS,
    val info: SpaceInfo? = null,
    /** Only filled for Mainframe Oldenburg. */
    val rooms: List<MainframeRoom> = emptyList(),
    val lastUpdate: Instant? = null,
    /** The last fetch's failure, or null. */
    val error: SpaceStateException? = null,
    /** True while a refresh the user asked for runs. */
    val isRefreshing: Boolean = false,
) {
    /** Unknown while the last fetch failed, so a stale state is never shown as current. */
    val status: SpaceStatus get() = if (error == null) info?.status ?: SpaceStatus.UNKNOWN else SpaceStatus.UNKNOWN
}

/** Keeps the selected space's status current, like `StatusStore` in the Apple apps. */
class StatusViewModel(
    private val source: StatusSource,
    private val settings: SettingsStore,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(StatusUiState())
    val state: StateFlow<StatusUiState> = _state.asStateFlow()

    private val fetching = Mutex()

    init {
        viewModelScope.launch {
            settings.settings.collect { settings ->
                _state.update {
                    if (it.endpoint == settings.endpoint) {
                        it.copy(refreshSeconds = settings.refreshSeconds)
                    } else {
                        StatusUiState(endpoint = settings.endpoint, refreshSeconds = settings.refreshSeconds)
                    }
                }
            }
        }
    }

    /**
     * Refreshes now and then after every interval, starting over when the space or the
     * interval changes. Runs while the caller is active, so polling pauses in the background
     * and catches up when the app comes back.
     */
    suspend fun poll() {
        settings.settings.distinctUntilChanged().collectLatest { settings ->
            while (true) {
                fetch(settings.endpoint)
                delay(settings.refreshSeconds * 1000L)
            }
        }
    }

    /** A refresh the user asked for, e.g. by pulling down. */
    fun refresh() {
        val endpoint = _state.value.endpoint ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                fetch(endpoint)
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun selectSpace(endpoint: String) {
        viewModelScope.launch { settings.setEndpoint(endpoint) }
    }

    fun setRefreshSeconds(seconds: Int) {
        viewModelScope.launch { settings.setRefreshSeconds(seconds) }
    }

    private suspend fun fetch(endpoint: String) = fetching.withLock {
        try {
            val (info, rooms) = coroutineScope {
                val info = async { source.space(endpoint) }
                // The rooms are extra detail; failing to load them must not hide the space's state.
                val rooms = if (source.isMainframe(endpoint)) roomsOrEmpty() else emptyList()
                info.await() to rooms
            }
            _state.update {
                if (it.endpoint != endpoint) it
                else it.copy(info = info, rooms = rooms, error = null, lastUpdate = now())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                if (it.endpoint != endpoint) it
                else it.copy(error = e as? SpaceStateException ?: SpaceStateException.Other(e.toString()), lastUpdate = now())
            }
        }
    }

    private suspend fun roomsOrEmpty(): List<MainframeRoom> = try {
        source.mainframeRooms()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SpaceStateApplication
                StatusViewModel(app.statusSource, app.settings)
            }
        }
    }
}

