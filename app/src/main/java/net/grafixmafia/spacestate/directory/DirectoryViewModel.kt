package net.grafixmafia.spacestate.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.grafixmafia.spacestate.SpaceStateApplication
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.DirectoryEntry

data class DirectoryUiState(
    val entries: List<DirectoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: SpaceStateException? = null,
)

/** The spaces to choose from, loaded on demand, like `DirectoryStore` in the Apple apps. */
class DirectoryViewModel(private val source: StatusSource) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryUiState())
    val state: StateFlow<DirectoryUiState> = _state.asStateFlow()

    /** Loads the list unless it is loaded or loading already. */
    fun loadIfNeeded() {
        if (_state.value.entries.isEmpty()) load()
    }

    fun load() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val entries = source.directory()
                _state.update { DirectoryUiState(entries = entries) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e as? SpaceStateException ?: SpaceStateException.Other(e.toString())) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DirectoryViewModel((this[APPLICATION_KEY] as SpaceStateApplication).statusSource)
            }
        }
    }
}

/** Spaces whose name or address contains the query, ignoring case and accents. */
fun List<DirectoryEntry>.matching(query: String): List<DirectoryEntry> {
    val needle = query.trim().folded()
    if (needle.isEmpty()) return this
    return filter { it.name.folded().contains(needle) || it.address?.folded()?.contains(needle) == true }
}

private val marks = Regex("\\p{Mn}+")

private fun String.folded(): String =
    marks.replace(Normalizer.normalize(this, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
