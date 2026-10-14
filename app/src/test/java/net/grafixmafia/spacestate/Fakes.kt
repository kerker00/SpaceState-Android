package net.grafixmafia.spacestate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Instant
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.ObservationSource
import net.grafixmafia.spacestatekit.StatusObservation
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
    var now: () -> Instant = { Instant.EPOCH }
    val spaceObservations = mutableMapOf<String, StatusObservation<SpaceInfo>>()
    var roomsObservation: StatusObservation<List<MainframeRoom>>? = null
    var directoryObservations: List<StatusObservation<DirectoryEntry>>? = null
    var roomGate: CompletableDeferred<Unit>? = null
    var ignoresCancellation = false

    override val mainframeEndpoint = MAINFRAME

    override fun isMainframe(endpoint: String) = endpoint == MAINFRAME

    override suspend fun space(endpoint: String): SpaceInfo {
        spaceReads += endpoint
        val answer = spaces[endpoint]
        val gate = gates[endpoint]
        if (gate != null && ignoresCancellation) withContext(NonCancellable) { gate.await() }
        else gate?.await()
        return when (answer) {
            is SpaceInfo -> answer
            is Throwable -> throw answer
            else -> throw IllegalStateException("no answer for $endpoint")
        }
    }

    override suspend fun mainframeRooms(): List<MainframeRoom> {
        roomReads++
        roomGate?.await()
        return rooms.getOrThrow()
    }

    override suspend fun directory(): List<DirectoryEntry> {
        directoryReads++
        return directory.getOrThrow()
    }

    override suspend fun spaceObservation(endpoint: String): StatusObservation<SpaceInfo> {
        val value = space(endpoint)
        return spaceObservations[endpoint] ?: observed(value)
    }

    override suspend fun mainframeRoomsObservation(): StatusObservation<List<MainframeRoom>> {
        val value = mainframeRooms()
        return roomsObservation ?: observed(value)
    }

    override suspend fun directoryObservations(): List<StatusObservation<DirectoryEntry>> {
        val values = directory()
        // Old name-only fixtures have no source evidence; production never fills it.
        return directoryObservations ?: values.map { StatusObservation(it,
            ObservationTiming(ObservationSource.AGGREGATOR, null, now(), null)) }
    }

    private fun <T> observed(value: T): StatusObservation<T> {
        val date = now()
        return StatusObservation(value, ObservationTiming(ObservationSource.DIRECT, date, date, date.plusSeconds(900)))
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
