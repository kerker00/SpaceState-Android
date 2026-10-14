package net.grafixmafia.spacestate.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.net.URI
import java.util.Date
import java.util.Locale
import java.time.Instant
import net.grafixmafia.spacestatekit.ObservationFreshness
import androidx.compose.runtime.remember
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.status.StatusUiState

/** The app's main screen: the selected space's state and, for Mainframe, its rooms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(state: StatusUiState, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.info?.name ?: state.endpoint?.let(::hostOf).orEmpty()) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
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
            LazyColumn(Modifier.fillMaxSize()) {
                item { Header(state) }
                if (state.info != null && (state.error != null || state.freshness != ObservationFreshness.CURRENT)) {
                    item { Text(stringResource(R.string.last_known_details), modifier = Modifier.padding(horizontal = 16.dp)) }
                }
                val message = state.info?.message
                if (message != null) {
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                if (state.rooms.isNotEmpty() || state.roomsTiming != null || state.roomsError != null) {
                    item {
                        SectionTitle(stringResource(R.string.rooms))
                        ObservationTime(R.string.observed, state.roomsTiming?.observedAt)
                        if (!state.roomsCurrent && state.rooms.isNotEmpty()) {
                            Text(stringResource(R.string.last_known_rooms), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        state.roomsError?.let { Text(it.text, modifier = Modifier.padding(16.dp)) }
                    }
                    if (state.rooms.isEmpty() && state.roomsError == null) {
                        item { Text(stringResource(R.string.no_rooms), modifier = Modifier.padding(16.dp)) }
                    }
                    items(state.displayRooms, key = { it.id }) { room ->
                        ListItem(
                            leadingContent = { StatusDot(room.state.color) },
                            headlineContent = { Text(room.name) },
                            trailingContent = { Text(room.stateTitle) },
                        )
                    }
                }
                state.error?.let { error ->
                    item {
                        HorizontalDivider()
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
                            headlineContent = { Text(error.text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        )
                    }
                }
                state.info?.website?.let { website ->
                    item {
                        val uriHandler = LocalUriHandler.current
                        HorizontalDivider()
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.website)) },
                            modifier = Modifier.clickableListItem { uriHandler.openUri(website) },
                        )
                    }
                }
                item { ObservationTime(R.string.observed, state.spaceTiming?.observedAt) }
                state.lastUpdate?.let { date -> item { ObservationTime(R.string.received, date) } }
                state.lastAttempt?.let { date -> item { ObservationTime(R.string.checked, date) } }
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
        modifier = Modifier.padding(16.dp),
    ) {
        Icon(status.icon, contentDescription = null, tint = status.color, modifier = Modifier.size(48.dp))
        Column {
            Text(status.title, style = MaterialTheme.typography.headlineSmall, color = status.color)
            val since = state.info?.lastChange
            if (since != null && since <= state.evaluatedAt && status != net.grafixmafia.spacestatekit.SpaceStatus.UNKNOWN) {
                val relative = DateUtils.getRelativeTimeSpanString(
                    since.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                ).toString().replaceFirstChar { it.lowercase(Locale.getDefault()) } // "Vor 5 Minuten" mid-sentence
                Text(
                    stringResource(R.string.changed, relative),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** The endpoint's host, shown until the space's name is known. */
fun hostOf(endpoint: String): String = runCatching { URI(endpoint).host }.getOrNull() ?: endpoint


/** Include date as well as time so a retained observation from yesterday is clear. */
@Composable
fun ObservationTime(label: Int, date: Instant?, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
