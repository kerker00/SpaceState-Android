package net.grafixmafia.spacestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import net.grafixmafia.spacestate.data.Settings
import net.grafixmafia.spacestate.data.SettingsStore
import net.grafixmafia.spacestate.data.StatusSource
import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.SpaceInfo
import net.grafixmafia.spacestatekit.SpaceStatus

const val MAINFRAME = "https://status.mainframe.io/api/spaceInfo"
const val LEINELAB = "https://leinelab.org/api/spaceapi.json"

fun space(endpoint: String, name: String, status: SpaceStatus = SpaceStatus.OPEN) =
    SpaceInfo(endpoint, name, status, lastChange = null, message = null, website = null, logo = null, address = null)

fun room(id: String, state: MainframeRoomState) = MainframeRoom(id, id, state, rawState = id, since = null)

class FakeSource : StatusSource {
    /** Answers by endpoint; a missing endpoint, or a [Throwable] value, fails the read. */
    val spaces = mutableMapOf<String, Any>()
    var rooms: Result<List<MainframeRoom>> = Result.success(emptyList())
    var directory: Result<List<DirectoryEntry>> = Result.success(emptyList())
    /** When set, reads of this endpoint wait for it. */
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
    val spaceReads = mutableListOf<String>()
    var roomReads = 0
    var directoryReads = 0

    override val mainframeEndpoint = MAINFRAME

    override fun isMainframe(endpoint: String) = endpoint == MAINFRAME

    override suspend fun space(endpoint: String): SpaceInfo {
        spaceReads += endpoint
        gates[endpoint]?.await()
        return when (val answer = spaces[endpoint]) {
            is SpaceInfo -> answer
            is Throwable -> throw answer
            else -> throw IllegalStateException("no answer for $endpoint")
        }
    }

    override suspend fun mainframeRooms(): List<MainframeRoom> {
        roomReads++
        return rooms.getOrThrow()
    }

    override suspend fun directory(): List<DirectoryEntry> {
        directoryReads++
        return directory.getOrThrow()
    }
}

class FakeSettings(endpoint: String = MAINFRAME, refreshSeconds: Int = 120) : SettingsStore {
    override val settings = MutableStateFlow(Settings(endpoint, refreshSeconds))

    override suspend fun setEndpoint(endpoint: String) {
        settings.value = settings.value.copy(endpoint = endpoint)
    }

    override suspend fun setRefreshSeconds(seconds: Int) {
        settings.value = settings.value.copy(refreshSeconds = seconds)
    }
}
