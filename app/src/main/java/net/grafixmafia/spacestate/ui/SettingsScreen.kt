package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpaceCard(onClick = onChooseSpace) {
                    SpaceHeading(stringResource(R.string.space))
                    Text(state.info?.name ?: state.endpoint?.let(::hostOf).orEmpty())
                }
                var choosingInterval by remember { mutableStateOf(false) }
                Box {
                    SpaceCard(onClick = { choosingInterval = true }) {
                        SpaceHeading(stringResource(R.string.check_every))
                        Text(minutes(state.refreshSeconds))
                    }
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
}

@Composable
private fun minutes(seconds: Int): String = pluralStringResource(R.plurals.minutes, seconds / 60, seconds / 60)

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}
