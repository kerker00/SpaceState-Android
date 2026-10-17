package net.grafixmafia.spacestate.details

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.grafixmafia.spacestate.FakeSettings
import net.grafixmafia.spacestate.FakeSource
import net.grafixmafia.spacestate.LEINELAB
import net.grafixmafia.spacestate.MAINFRAME
import net.grafixmafia.spacestate.TestViewModels
import net.grafixmafia.spacestate.data.TemporarySettings
import net.grafixmafia.spacestate.space
import net.grafixmafia.spacestate.status.StatusViewModel
import net.grafixmafia.spacestatekit.SpaceDetails
import net.grafixmafia.spacestatekit.SpaceStateException
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailLinksTest {
    @Test fun webLinksPreservePathsQueriesAndValidPorts() {
        assertEquals("https://a.example:8443/p?a=1&b=2#x", DetailLinks.web("https://a.example:8443/p?a=1&b=2#x"))
        assertNotNull(DetailLinks.endpoint("http://localhost:8080/api"))
        assertNotNull(DetailLinks.endpoint("http://[::1]:8080/api"))
    }
    @Test fun unsafeSchemesCredentialsAndPortsNeverBecomeActions() {
        listOf("javascript:alert(1)", "file:///tmp/x", "/relative", "https://u:p@a.example/", "https://a.example:0/",
            "https://a.example:65536/", "https://a.example:/", "https://a.example/a%0a", "https://a.example/a%C2%80", "https://a.example/a\n").forEach {
            assertNull(it, DetailLinks.web(it))
        }
    }
    @Test fun emailAddressesCannotInjectHeadersOrOtherSchemes() {
        assertEquals("mailto:a@example.org", DetailLinks.contact("email", "a@example.org"))
        listOf("a@example.org?body=bad", "a@example.org%0d%0abcc:b@example.org", "javascript:bad@example.org", "bad address@example.org").forEach {
            assertNull(DetailLinks.contact("email", it))
        }
    }
    @Test fun phoneFormattingIsNormalizedWithoutAcceptingDialCommands() {
        assertEquals("tel:+49123", DetailLinks.contact("phone", "+49 (123)"))
        assertNull(DetailLinks.contact("phone", "+49#123"))
        assertNull(DetailLinks.contact("phone", "Call the front desk"))
    }
    @Test fun contactProtocolsUseTheirOwnAllowlists() {
        assertNotNull(DetailLinks.contact("irc", "ircs://chat.example/room"))
        assertNotNull(DetailLinks.contact("mumble", "mumble://voice.example/"))
        assertEquals("xmpp:a@example.org", DetailLinks.contact("xmpp", "a@example.org"))
        assertNull(DetailLinks.contact("irc", "file:///tmp/x"))
        assertNull(DetailLinks.contact("mumble", "mumble://user:password@voice.example/"))
    }
    @Test fun matrixAndMastodonIdentifiersBecomeBoundedWebDestinations() {
        assertEquals("https://matrix.to/#/%23room%3Aexample.org", DetailLinks.contact("matrix", "#room:example.org"))
        assertEquals("https://example.org/@ada", DetailLinks.contact("mastodon", "@ada@example.org"))
        assertNull(DetailLinks.contact("matrix", "#room:example.org\n"))
        assertNull(DetailLinks.contact("mastodon", "@ada@evil.example/path"))
    }
    @Test fun sipAndMailingListValuesDoNotNeedWebSchemes() {
        assertEquals("sip:+49123", DetailLinks.contact("sip", "+49 123"))
        assertEquals("sips:a@example.org", DetailLinks.contact("sip", "sips:a@example.org"))
        assertEquals("mailto:list@example.org", DetailLinks.contact("mailingList", "list@example.org"))
        assertNotNull(DetailLinks.contact("mailingList", "https://lists.example/list"))
    }
    @Test fun mapTargetsRequireAPairedFiniteCoordinateWithinBounds() {
        assertEquals("geo:53.0,8.0?q=53.0,8.0", DetailLinks.map(53.0, 8.0))
        assertNull(DetailLinks.map(null, 8.0)); assertNull(DetailLinks.map(53.0, null))
        assertNull(DetailLinks.map(Double.NaN, 8.0)); assertNull(DetailLinks.map(91.0, 8.0))
        assertNull(DetailLinks.map(53.0, Double.POSITIVE_INFINITY)); assertNull(DetailLinks.map(53.0, -181.0))
    }
    @Test fun unknownContactKindsCannotStartAnExternalAction() {
        assertNull(DetailLinks.contact("future", "https://a.example/"))
        assertNull(DetailLinks.web(""))
    }
    @Test fun temporarySettingsIgnoreSelectionAndIntervalWrites() = runTest {
        val settings = TemporarySettings(LEINELAB, 300)
        settings.setEndpoint(MAINFRAME); settings.setRefreshSeconds(900)
        assertEquals(LEINELAB, settings.settings.first().endpoint)
        assertEquals(300, settings.settings.first().refreshSeconds)
        assertEquals(120, TemporarySettings(LEINELAB, -1).settings.first().refreshSeconds)
    }
    @Test fun linkedViewModelDoesNotReplaceThePrimarySelection() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val models = TestViewModels()
            models.run(dispatcher) {
                val source = FakeSource().also { it.now = { Instant.ofEpochMilli(testScheduler.currentTime) } }
                source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
                source.spaces[LEINELAB] = space(LEINELAB, "LeineLab")
                val saved = FakeSettings()
                val primary = models.track(StatusViewModel(source, saved, now = source.now))
                val linked = models.track(StatusViewModel(source, TemporarySettings(LEINELAB, 120), now = source.now))
                runCurrent(); primary.refresh(); linked.refresh(); runCurrent()
                assertEquals(MAINFRAME, primary.state.value.endpoint)
                assertEquals(LEINELAB, linked.state.value.endpoint)
                linked.selectSpace(MAINFRAME); runCurrent()
                assertEquals(LEINELAB, linked.state.value.endpoint)
                assertEquals(MAINFRAME, saved.settings.value.endpoint)
            }
        } finally { Dispatchers.resetMain() }
    }
    @Test fun failedRefreshRetainsDetailsWithTheirOriginalObservation() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val models = TestViewModels()
            models.run(dispatcher) {
                val source = FakeSource().also { it.now = { Instant.ofEpochMilli(testScheduler.currentTime) } }
                source.spaces[LEINELAB] = space(LEINELAB, "LeineLab").copy(details = SpaceDetails(accessHint = "Ring"))
                val model = models.track(StatusViewModel(source, TemporarySettings(LEINELAB, 120), now = source.now))
                runCurrent(); model.refresh(); runCurrent()
                val timing = model.state.value.spaceTiming
                source.spaces[LEINELAB] = SpaceStateException.Unreachable()
                model.refresh(); runCurrent()
                assertEquals("Ring", model.state.value.info?.details?.accessHint)
                assertEquals(timing, model.state.value.spaceTiming)
                assertNotNull(model.state.value.error)
            }
        } finally { Dispatchers.resetMain() }
    }
}
