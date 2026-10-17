package net.grafixmafia.spacestate.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.grafixmafia.spacestate.SpaceStateApplication
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus

data class DirectoryUiState(
    val entries: List<DirectoryEntry> = emptyList(),
    val timings: Map<String, ObservationTiming> = emptyMap(),
    val isLoading: Boolean = false,
    val error: SpaceStateException? = null,
    val lastAttempt: Instant? = null,
    val lastUpdate: Instant? = null,
    val evaluatedAt: Instant = Instant.EPOCH,
) {
    val displayEntries: List<DirectoryEntry> get() = entries.map {
        it.copy(status = if (error == null) timings[it.endpoint]?.currentStatus(it.status, evaluatedAt) ?: SpaceStatus.UNKNOWN else SpaceStatus.UNKNOWN)
    }
}

class DirectoryViewModel(private val source: StatusSource, private val now: () -> Instant = Instant::now) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryUiState())
    val state: StateFlow<DirectoryUiState> = _state.asStateFlow()
    private var expiryJob: Job? = null

    fun loadIfNeeded() {
        recheck()
        if (_state.value.entries.isEmpty()) load()
    }

    fun recheck() {
        _state.update { it.copy(evaluatedAt = now()) }
        armExpiry()
    }

    fun load() {
        if (_state.value.isLoading) return
        val attempt = now()
        _state.update { it.copy(isLoading = true, lastAttempt = attempt, evaluatedAt = attempt) }
        viewModelScope.launch {
            try {
                val observations = source.directoryObservations().distinctBy { it.value.endpoint }
                val date = now()
                _state.update { it.copy(entries = observations.map { it.value }, timings = observations.associate { it.value.endpoint to it.timing },
                    isLoading = false, error = null, lastUpdate = date, evaluatedAt = date) }
            } catch (error: CancellationException) {
                _state.update { it.copy(isLoading = false) }
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(isLoading = false, error = error as? SpaceStateException ?: SpaceStateException.Other(error.toString()), evaluatedAt = now()) }
            } finally { armExpiry() }
        }
    }

    private fun armExpiry() {
        expiryJob?.cancel()
        expiryJob = viewModelScope.launch {
            while (true) {
                val date = now()
                _state.update { it.copy(evaluatedAt = date) }
                val expiry = _state.value.timings.values.mapNotNull { it.expiresAt }.filter { it > date }.minOrNull() ?: return@launch
                delay(Duration.between(date, expiry).toMillis().coerceIn(1L, 60_000L))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer {
            DirectoryViewModel((this[APPLICATION_KEY] as SpaceStateApplication).statusSource)
        } }
    }
}

/** Names remain searchable even when their opening observations are no longer current. */
fun List<DirectoryEntry>.matching(query: String): List<DirectoryEntry> {
    val needle = query.trim().folded()
    if (needle.isEmpty()) return this
    return filter { it.name.folded().contains(needle) || it.address?.folded()?.contains(needle) == true }
}

private val marks = Regex("\\p{Mn}+")
private fun String.folded(): String = marks.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
