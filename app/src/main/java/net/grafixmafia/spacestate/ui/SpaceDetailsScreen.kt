package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.grafixmafia.spacestate.R
import net.grafixmafia.spacestate.details.DetailLinks
import net.grafixmafia.spacestate.status.StatusUiState
import net.grafixmafia.spacestatekit.DetailContact
import net.grafixmafia.spacestatekit.ObservationFreshness

/** Optional details use the same retained observation as the parent status view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailsScreen(state: StatusUiState, onBack: () -> Unit, onLinkedSpace: (String) -> Unit) {
    val info = state.info
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val handler = LocalUriHandler.current
    val cannotOpen = stringResource(R.string.cannot_open_link)
    val open: (String) -> Unit = { uri ->
        runCatching { handler.openUri(uri) }.onFailure { scope.launch { snackbar.showSnackbar(cannotOpen) } }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.space_details)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SpaceCard {
                        SpaceHeading(info?.name ?: state.endpoint?.let(::hostOf).orEmpty())
                        if (state.error != null || state.freshness != ObservationFreshness.CURRENT) {
                            Text(stringResource(R.string.last_known_details), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ObservationTime(R.string.observed, state.spaceTiming?.observedAt)
                        state.lastUpdate?.let { ObservationTime(R.string.received, it) }
                        state.lastAttempt?.let { ObservationTime(R.string.checked, it) }
                    }
                }
                if (info != null) {
                    val details = info.details
                    info.address?.takeIf { it.isNotBlank() }?.let { item { DetailValue(stringResource(R.string.address), it) } }
                    details.accessHint?.let { item { DetailValue(stringResource(R.string.access), it) } }
                    details.countryCode?.let { item { DetailValue(stringResource(R.string.country), it) } }
                    details.timezone?.let { item { DetailValue(stringResource(R.string.timezone), it) } }
                    DetailLinks.map(details.latitude, details.longitude)?.let { uri ->
                        item { DetailValue(stringResource(R.string.coordinates), "${details.latitude}, ${details.longitude}", uri, open) }
                    }
                    info.website?.let { item { DetailValue(stringResource(R.string.website), it, DetailLinks.web(it), open) } }
                    items(details.contacts) { field -> ContactValue(field, open) }
                    items(details.keyholders) { person ->
                      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpaceCard {
                            SpaceHeading(stringResource(R.string.keyholder))
                            person.name?.let { SelectionContainer { Text(it) } }
                            person.ircNick?.let { Text(stringResource(R.string.irc_nickname, it)) }
                        }
                        person.contacts.forEach { ContactValue(it, open) }
                      }
                    }
                    items(details.feeds) { feed ->
                        val title = when (feed.kind) {
                            "blog" -> R.string.blog; "wiki" -> R.string.wiki; "calendar" -> R.string.calendar
                            "flickr" -> R.string.flickr; else -> null
                        }
                        DetailValue(title?.let { stringResource(it) } ?: feed.kind, feed.url, DetailLinks.web(feed.url), open, feed.format)
                    }
                    items(details.projects) { url -> DetailValue(stringResource(R.string.projects), url, DetailLinks.web(url), open) }
                    items(details.links) { link -> DetailValue(link.name, link.url, DetailLinks.web(link.url), open, link.summary) }
                    items(details.linkedSpaces) { linked ->
                        SpaceCard {
                            SpaceHeading(stringResource(R.string.linked_space))
                            linked.apiEndpoint?.let { endpoint ->
                                SelectionContainer { Text(endpoint) }
                                DetailLinks.endpoint(endpoint)?.let { validated ->
                                    TextButton(onClick = { onLinkedSpace(validated) }) { Text(stringResource(R.string.open_space)) }
                                }
                            }
                            linked.website?.let { url ->
                                SelectionContainer { Text(url) }
                                DetailLinks.web(url)?.let { validated ->
                                    TextButton(onClick = { open(validated) }) { Text(stringResource(R.string.website)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailValue(title: String, value: String, uri: String? = null, open: (String) -> Unit = {}, note: String? = null) {
    SpaceCard(onClick = uri?.let { { open(it) } }) {
        SpaceHeading(title)
        SelectionContainer { Text(value) }
        note?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (uri != null) Text(stringResource(R.string.open_link), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ContactValue(field: DetailContact, open: (String) -> Unit) {
    val title = when (field.kind) {
        "email" -> R.string.email; "phone" -> R.string.phone; "irc" -> R.string.irc
        "mailingList" -> R.string.mailing_list; "matrix" -> R.string.matrix; "mastodon" -> R.string.mastodon
        "xmpp" -> R.string.xmpp; "mumble" -> R.string.mumble; "sip" -> R.string.sip
        "issueMail" -> R.string.report_issue; "gopher" -> R.string.gopher; else -> null
    }
    DetailValue(title?.let { stringResource(it) } ?: field.kind, field.value, DetailLinks.contact(field.kind, field.value), open)
}
