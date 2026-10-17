package net.grafixmafia.spacestate.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.net.URI
import java.time.Instant
import java.util.Date
import java.util.Locale
import net.grafixmafia.spacestate.details.DetailLinks
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.status.StatusUiState
import net.grafixmafia.spacestatekit.ObservationFreshness
import net.grafixmafia.spacestatekit.SpaceStatus

/** Selected space and independent room evidence, on the shared card surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    state: StatusUiState, onRefresh: () -> Unit, onOpenSettings: () -> Unit,
    onOpenDetails: (() -> Unit)? = null, onBack: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = { onBack?.let { BackButton(it) } },
                actions = {
                  if (onBack == null) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                  }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SpaceCard {
                            SpaceHeading(state.info?.name ?: state.endpoint?.let(::hostOf).orEmpty())
                            Header(state)
                            if (state.info != null && (state.error != null || state.freshness != ObservationFreshness.CURRENT)) {
                                Text(stringResource(R.string.last_known_details), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.info?.message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                    state.error?.let { error ->
                        item { SpaceErrorCard(error.text, onRefresh, enabled = !state.isRefreshing) }
                    }
                    if (state.rooms.isNotEmpty() || state.roomsTiming != null || state.roomsError != null) {
                        item {
                            SpaceCard {
                                SpaceHeading(stringResource(R.string.rooms))
                                ObservationTime(R.string.observed, state.roomsTiming?.observedAt)
                                state.roomsLastUpdate?.let { ObservationTime(R.string.received, it) }
                                state.roomsLastAttempt?.let { ObservationTime(R.string.checked, it) }
                                if (!state.roomsCurrent && state.rooms.isNotEmpty()) {
                                    Text(stringResource(R.string.last_known_rooms), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (state.rooms.isEmpty() && state.roomsError == null) {
                                    Text(stringResource(R.string.no_rooms))
                                }
                                state.roomsError?.let { error ->
                                    Text(error.text, color = MaterialTheme.colorScheme.onSurface)
                                    TextButton(onClick = onRefresh, enabled = !state.isRefreshing) {
                                        Text(stringResource(R.string.try_again))
                                    }
                                }
                                state.displayRooms.forEach { room ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.semantics(mergeDescendants = true) {},
                                    ) {
                                        Text(room.name, style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            StatusDot(room.state.color)
                                            Text(room.stateTitle, color = room.state.color, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (state.info != null && onOpenDetails != null) {
                        item { OutlinedButton(onClick = onOpenDetails) { Text(stringResource(R.string.space_details)) } }
                    }
                    DetailLinks.web(state.info?.website)?.let { website ->
                        item {
                            val uriHandler = LocalUriHandler.current
                            OutlinedButton(onClick = { uriHandler.openUri(website) }) {
                                Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.website))
                            }
                        }
                    }
                    item {
                        SpaceCard {
                            ObservationTime(R.string.observed, state.spaceTiming?.observedAt)
                            state.lastUpdate?.let { ObservationTime(R.string.received, it) }
                            state.lastAttempt?.let { ObservationTime(R.string.checked, it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: StatusUiState) {
    val status = state.status
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        // At large font sizes the status word has priority over a decorative symbol.
        if (LocalDensity.current.fontScale < 1.5f) {
            Icon(status.icon, contentDescription = null, tint = status.color, modifier = Modifier.size(48.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(status.title, style = MaterialTheme.typography.headlineMedium, color = status.color)
            val since = state.info?.lastChange
            if (since != null && since <= state.evaluatedAt && status != SpaceStatus.UNKNOWN) {
                val relative = DateUtils.getRelativeTimeSpanString(
                    since.toEpochMilli(), state.evaluatedAt.toEpochMilli(), DateUtils.MINUTE_IN_MILLIS,
                ).toString().replaceFirstChar { it.lowercase(Locale.getDefault()) }
                Text(
                    stringResource(R.string.changed, relative),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The endpoint's host, shown until the space's name is known. */
fun hostOf(endpoint: String): String = runCatching { URI(endpoint).host }.getOrNull() ?: endpoint

/** Include the date so a retained observation from yesterday is clear. */
@Composable
fun ObservationTime(label: Int, date: Instant?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formatted = remember(context, date) {
        date?.let { runCatching {
            DateFormat.getMediumDateFormat(context).format(Date.from(it)) + ", " + DateFormat.getTimeFormat(context).format(Date.from(it))
        }.getOrNull() }
    }
    Text(
        if (formatted != null) stringResource(label, formatted) else stringResource(R.string.no_observation),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
