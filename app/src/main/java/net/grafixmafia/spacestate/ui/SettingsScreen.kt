package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.data.SettingsStore
import net.grafixmafia.spacestate.status.StatusUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: StatusUiState,
    onChooseSpace: () -> Unit,
    onRefreshSecondsChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.space)) },
                supportingContent = { Text(state.info?.name ?: state.endpoint?.let(::hostOf).orEmpty()) },
                modifier = Modifier.clickableListItem(onChooseSpace),
            )
            var choosingInterval by remember { mutableStateOf(false) }
            Box {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.check_every)) },
                    supportingContent = { Text(minutes(state.refreshSeconds)) },
                    modifier = Modifier.clickableListItem { choosingInterval = true },
                )
                DropdownMenu(expanded = choosingInterval, onDismissRequest = { choosingInterval = false }) {
                    SettingsStore.refreshIntervals.forEach { seconds ->
                        DropdownMenuItem(
                            text = { Text(minutes(seconds)) },
                            onClick = {
                                choosingInterval = false
                                onRefreshSecondsChange(seconds)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun minutes(seconds: Int): String = pluralStringResource(R.plurals.minutes, seconds / 60, seconds / 60)

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}
