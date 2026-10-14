package net.grafixmafia.spacestate.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.Instant
import net.grafixmafia.spacestate.directory.DirectoryUiState
import net.grafixmafia.spacestate.status.StatusUiState
import net.grafixmafia.spacestate.ui.theme.SpaceStateTheme
import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.ObservationSource
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.SpaceInfo
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus

// Debug-only examples: no ViewModels, native clients, settings writes or network calls.
private val now = Instant.parse("2026-10-14T12:00:00Z")
private val timing = ObservationTiming(ObservationSource.SPACE_PUSH, now.minusSeconds(120), now.minusSeconds(30), now.plusSeconds(780))
private val endpoint = "https://status.mainframe.io/api/spaceInfo"
private val rooms = listOf(
    MainframeRoom("space", "Space", MainframeRoomState.OPEN_PLUS, "open+", null),
    MainframeRoom("radstelle", "Radstelle", MainframeRoomState.MEMBER, "member", null),
    MainframeRoom("lab3d", "3D Lab", MainframeRoomState.CLOSED, "closed", null),
    MainframeRoom("machining", "Machining", MainframeRoomState.KEYHOLDER, "keyholder", null),
    MainframeRoom("woodworking", "Woodworking", MainframeRoomState.CLOSING, "closing", null),
)
private fun sampleState(failed: Boolean = false) = StatusUiState(
    endpoint = endpoint,
    info = SpaceInfo(endpoint, "Mainframe Oldenburg", SpaceStatus.OPEN, now.minusSeconds(3600),
        "Lange Meldung des Spaces: Werkstatt und Gemeinschaftsraum sind heute geöffnet.", null, null, null),
    rooms = rooms,
    spaceTiming = timing, roomsTiming = timing,
    lastUpdate = timing.receivedAt, lastAttempt = now,
    roomsLastUpdate = timing.receivedAt, roomsLastAttempt = now,
    error = if (failed) SpaceStateException.Unreachable() else null,
    roomsError = if (failed) SpaceStateException.Timeout() else null,
    evaluatedAt = if (failed) now.plusSeconds(1800) else now,
)

@Preview(name = "Status DE light", locale = "de", widthDp = 360, heightDp = 920, showBackground = true)
@Preview(name = "Status EN dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 920, showBackground = true)
@Preview(name = "Status DE large font", locale = "de", fontScale = 2f, widthDp = 320, heightDp = 920, showBackground = true)
@Composable
private fun StatusAppearancePreview() {
    SpaceStateTheme(dynamicColor = false) { StatusScreen(sampleState(), {}, {}) }
}

@Preview(name = "Retained errors DE", locale = "de", fontScale = 1.5f, widthDp = 360, heightDp = 920, showBackground = true)
@Preview(name = "Retained errors EN dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 920, showBackground = true)
@Composable
private fun ErrorAppearancePreview() {
    SpaceStateTheme(dynamicColor = false) { StatusScreen(sampleState(failed = true), {}, {}) }
}

@Preview(name = "System colors", locale = "de", widthDp = 360, heightDp = 920, showBackground = true)
@Composable
private fun SystemColorPreview() {
    SpaceStateTheme(dynamicColor = true) { StatusScreen(sampleState(), {}, {}) }
}

@Preview(name = "Picker DE light", locale = "de", widthDp = 360, heightDp = 920, showBackground = true)
@Preview(name = "Picker EN dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 920, showBackground = true)
@Preview(name = "Picker DE large font", locale = "de", fontScale = 2f, widthDp = 320, heightDp = 920, showBackground = true)
@Composable
private fun PickerAppearancePreview() {
    val entries = listOf(
        DirectoryEntry(endpoint, "Mainframe Oldenburg", "Bahnhofsplatz 10, 26122 Oldenburg", SpaceStatus.OPEN),
        DirectoryEntry("https://long.example/api", "Hackerspace mit einem sehr langen Namen", "Eine sehr lange Adresse mit einem zusätzlichen Zugangshinweis, Beispielstadt", SpaceStatus.CLOSED),
        DirectoryEntry("https://unknown.example/api", "Space ohne aktuellen Stand", null, SpaceStatus.OPEN),
    )
    val directory = DirectoryUiState(entries = entries, timings = mapOf(endpoint to timing, entries[1].endpoint to timing), evaluatedAt = now)
    SpaceStateTheme(dynamicColor = false) {
        SpacePickerScreen(directory, endpoint, {}, {}, {}, {})
    }
}

@Preview(name = "Picker retained error DE", locale = "de", fontScale = 2f, widthDp = 320, heightDp = 920, showBackground = true)
@Composable
private fun PickerErrorAppearancePreview() {
    val directory = DirectoryUiState(
        entries = listOf(DirectoryEntry(endpoint, "Mainframe Oldenburg", "Bahnhofsplatz 10, Oldenburg", SpaceStatus.OPEN)),
        timings = mapOf(endpoint to timing), error = SpaceStateException.Unreachable(), evaluatedAt = now,
    )
    SpaceStateTheme(dynamicColor = false) { SpacePickerScreen(directory, endpoint, {}, {}, {}, {}) }
}

@Preview(name = "Settings DE large font", locale = "de", fontScale = 2f, widthDp = 320, heightDp = 760, showBackground = true)
@Preview(name = "Settings EN dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun SettingsAppearancePreview() {
    SpaceStateTheme(dynamicColor = false) {
        SettingsScreen(sampleState().let { it.copy(info = it.info?.copy(name = "Hackerspace mit einem sehr langen Namen")) }, {}, {}, {})
    }
}
