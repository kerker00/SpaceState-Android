package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.directory.DirectoryUiState
import net.grafixmafia.spacestate.directory.matching
import net.grafixmafia.spacestatekit.DirectoryEntry

/** Search and selection retain directory freshness and native input/navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacePickerScreen(
    directory: DirectoryUiState,
    selectedEndpoint: String?,
    onAppear: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onAppear() }
    var query by rememberSaveable { mutableStateOf("") }
    val displayed = directory.displayEntries
    val entries = remember(displayed, query) { displayed.matching(query) }
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_space)) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = onRetry, enabled = !directory.isLoading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_spaces)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (directory.entries.isEmpty() && directory.isLoading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            directory.error?.let { error ->
                                item { SpaceErrorCard(error.text, onRetry, enabled = !directory.isLoading) }
                            }
                            if (entries.isEmpty() && directory.error == null) {
                                item { SpaceCard { Text(stringResource(R.string.no_spaces_found)) } }
                            }
                            items(entries, key = { it.endpoint }) { entry ->
                                SpaceSelectionCard(
                                    entry = entry,
                                    observedAt = directory.timings[entry.endpoint]?.observedAt,
                                    isSelected = entry.endpoint == selectedEndpoint,
                                    onSelect = { onSelect(entry.endpoint) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceSelectionCard(
    entry: DirectoryEntry,
    observedAt: java.time.Instant?,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    SpaceCard(onClick = onSelect, isSelected = isSelected) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        entry.address?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(entry.status.icon, contentDescription = null, tint = entry.status.color, modifier = Modifier.size(20.dp))
            Text(entry.status.title, color = entry.status.color)
        }
        ObservationTime(R.string.observed, observedAt)
    }
}
