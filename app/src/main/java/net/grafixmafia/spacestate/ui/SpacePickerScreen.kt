package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestate.directory.DirectoryUiState
import net.grafixmafia.spacestate.directory.matching

/** Every space in the SpaceAPI directory, searchable by name and address. */
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_spaces)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // The list filters while typing; the search key only makes room for it.
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (directory.error != null && directory.entries.isNotEmpty()) {
                Text(directory.error.text, modifier = Modifier.padding(16.dp))
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    directory.entries.isEmpty() && directory.isLoading ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    directory.entries.isEmpty() && directory.error != null ->
                        Unavailable(directory.error, onRetry, Modifier.align(Alignment.Center))
                    else -> LazyColumn {
                        items(entries, key = { it.endpoint }) { entry ->
                            ListItem(
                                leadingContent = { StatusDot(entry.status.color) },
                                headlineContent = { Text(entry.name) },
                                supportingContent = {
                                    Column {
                                        entry.address?.let { Text(it) }
                                        Text(entry.status.title)
                                        ObservationTime(R.string.observed, directory.timings[entry.endpoint]?.observedAt, Modifier)
                                    }
                                },
                                trailingContent = if (entry.endpoint == selectedEndpoint) {
                                    {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = stringResource(R.string.selected),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                } else null,
                                modifier = Modifier.clickableListItem { onSelect(entry.endpoint) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Unavailable(error: SpaceStateException, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(32.dp),
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null)
        Text(stringResource(R.string.spaces_unavailable), style = MaterialTheme.typography.titleMedium)
        Text(
            error.text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.try_again)) }
    }
}
