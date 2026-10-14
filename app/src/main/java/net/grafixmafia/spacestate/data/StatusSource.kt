package net.grafixmafia.spacestate.data

import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.MainframeRoom
import net.grafixmafia.spacestatekit.SpaceInfo
import net.grafixmafia.spacestatekit.SpaceStateClient
import net.grafixmafia.spacestatekit.StatusObservation

/** Where the app reads the spaces' state; an interface so tests run without SpaceStateKit's native code. */
interface StatusSource {
    val mainframeEndpoint: String

    fun isMainframe(endpoint: String): Boolean

    suspend fun space(endpoint: String): SpaceInfo

    suspend fun mainframeRooms(): List<MainframeRoom>

    suspend fun directory(): List<DirectoryEntry>

    suspend fun spaceObservation(endpoint: String): StatusObservation<SpaceInfo>
    suspend fun mainframeRoomsObservation(): StatusObservation<List<MainframeRoom>>
    suspend fun directoryObservations(): List<StatusObservation<DirectoryEntry>>
}

/** SpaceStateKit's `StatusService`: SpacePush first, the spaces directly as fallback. */
class KitStatusSource(private val client: SpaceStateClient) : StatusSource {
    override val mainframeEndpoint: String get() = SpaceStateClient.mainframeEndpoint

    override fun isMainframe(endpoint: String) = SpaceStateClient.isMainframe(endpoint)

    override suspend fun space(endpoint: String) = client.space(endpoint)

    override suspend fun mainframeRooms() = client.mainframeRooms()

    override suspend fun directory() = client.directory()

    override suspend fun spaceObservation(endpoint: String) = client.spaceObservation(endpoint)
    override suspend fun mainframeRoomsObservation() = client.mainframeRoomsObservation()
    override suspend fun directoryObservations() = client.directoryObservations()
}
