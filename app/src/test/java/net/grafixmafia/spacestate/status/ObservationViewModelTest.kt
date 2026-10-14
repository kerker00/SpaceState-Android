package net.grafixmafia.spacestate.status

import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.grafixmafia.spacestate.FakeSettings
import net.grafixmafia.spacestate.FakeSource
import net.grafixmafia.spacestate.MAINFRAME
import net.grafixmafia.spacestate.LEINELAB
import net.grafixmafia.spacestate.space
import net.grafixmafia.spacestate.room
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.ObservationSource
import net.grafixmafia.spacestatekit.ObservationFreshness
import net.grafixmafia.spacestatekit.StatusObservation
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ObservationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val source = FakeSource()
    private val settings = FakeSettings()
    private fun timing(observed: Long? = 0, expiry: Long? = 900) = ObservationTiming(
        ObservationSource.SPACE_PUSH, observed?.let(Instant::ofEpochSecond), Instant.EPOCH, expiry?.let(Instant::ofEpochSecond))
    private fun TestScope.model(): StatusViewModel {
        source.now = { Instant.ofEpochMilli(testScheduler.currentTime) }
        source.spaces[MAINFRAME] = space(MAINFRAME, "A")
        return StatusViewModel(source, settings, source.now).also { runCurrent() }
    }
    @Before fun start() = Dispatchers.setMain(dispatcher)
    @After fun stop() = Dispatchers.resetMain()

    @Test fun expiresBeforeTheNextPollWithoutChangingSourceOrAttempt() = runTest(dispatcher) {
        val model = model()
        source.spaceObservations[MAINFRAME] = StatusObservation(space(MAINFRAME, "A"), timing(expiry = 2))
        settings.settings.value = settings.settings.value.copy(refreshSeconds = 900)
        runCurrent()
        val polling = launch { model.poll() }; runCurrent()
        val original = model.state.value
        advanceTimeBy(2000); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(ObservationFreshness.STALE, model.state.value.freshness)
        assertEquals(original.spaceTiming, model.state.value.spaceTiming)
        assertEquals(original.lastAttempt, model.state.value.lastAttempt)
        assertEquals(1, source.spaceReads.size)
        polling.cancel()
    }

    @Test fun failedSpaceReadKeepsOriginalEvidenceAndSuccessfulCompletion() = runTest(dispatcher) {
        val model = model(); model.refresh(); runCurrent()
        val original = model.state.value
        advanceTimeBy(1000); runCurrent()
        source.spaces[MAINFRAME] = SpaceStateException.Unreachable()
        model.refresh(); runCurrent()
        assertEquals(original.lastUpdate, model.state.value.lastUpdate)
        assertEquals(original.spaceTiming, model.state.value.spaceTiming)
        assertEquals(Instant.ofEpochSecond(1), model.state.value.lastAttempt)
        assertEquals("A", model.state.value.info?.name)
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
    }

    @Test fun failedRoomsKeepNamesAndMakeOnlyTheirStatesUnknown() = runTest(dispatcher) {
        val model = model()
        source.rooms = Result.success(listOf(room("space", MainframeRoomState.OPEN_PLUS)))
        model.refresh(); runCurrent()
        val original = model.state.value
        source.rooms = Result.failure(SpaceStateException.Timeout())
        model.refresh(); runCurrent()
        assertEquals(SpaceStatus.OPEN, model.state.value.status)
        assertEquals(original.rooms, model.state.value.rooms)
        assertEquals(original.roomsTiming, model.state.value.roomsTiming)
        assertEquals(original.roomsLastUpdate, model.state.value.roomsLastUpdate)
        assertEquals(MainframeRoomState.UNKNOWN, model.state.value.displayRooms.single().state)
        assertEquals("", model.state.value.displayRooms.single().rawState)
        assertTrue(model.state.value.roomsError is SpaceStateException.Timeout)
    }

    @Test fun freshRoomsRemainCurrentWhenTheSpaceFails() = runTest(dispatcher) {
        val model = model()
        source.spaces[MAINFRAME] = SpaceStateException.Unreachable()
        source.rooms = Result.success(listOf(room("space", MainframeRoomState.OPEN)))
        model.refresh(); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(MainframeRoomState.OPEN, model.state.value.displayRooms.single().state)
        assertNull(model.state.value.lastUpdate)
        assertEquals(Instant.EPOCH, model.state.value.roomsLastUpdate)
    }

    @Test fun roomExpiryIsIndependentOfSpaceExpiry() = runTest(dispatcher) {
        val model = model()
        val rooms = listOf(room("space", MainframeRoomState.OPEN))
        source.rooms = Result.success(rooms)
        source.roomsObservation = StatusObservation(rooms, timing(expiry = 1))
        source.spaceObservations[MAINFRAME] = StatusObservation(space(MAINFRAME, "A"), timing(expiry = 3))
        model.refresh(); runCurrent()
        advanceTimeBy(1000); runCurrent()
        assertEquals(SpaceStatus.OPEN, model.state.value.status)
        assertEquals(MainframeRoomState.UNKNOWN, model.state.value.displayRooms.single().state)
        advanceTimeBy(2000); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(1, source.spaceReads.size)
    }

    @Test fun aSlowRoomReadDoesNotDelayPublishingTheSpace() = runTest(dispatcher) {
        val model = model(); source.roomGate = CompletableDeferred()
        model.refresh(); runCurrent()
        assertEquals(SpaceStatus.OPEN, model.state.value.status)
        assertTrue(model.state.value.isRefreshing)
        source.roomGate!!.complete(Unit); runCurrent()
        assertFalse(model.state.value.isRefreshing)
    }

    @Test fun sourceObservationReceiptCompletionAndAttemptStaySeparate() = runTest(dispatcher) {
        val model = model()
        advanceTimeBy(1000); runCurrent()
        source.spaceObservations[MAINFRAME] = StatusObservation(space(MAINFRAME, "A"), ObservationTiming(
            ObservationSource.SPACE_PUSH, Instant.EPOCH, Instant.ofEpochMilli(500), Instant.ofEpochSecond(5)))
        model.refresh(); runCurrent()
        assertEquals(Instant.EPOCH, model.state.value.spaceTiming?.observedAt)
        assertEquals(Instant.ofEpochMilli(500), model.state.value.spaceTiming?.receivedAt)
        assertEquals(Instant.ofEpochSecond(1), model.state.value.lastUpdate)
        assertEquals(Instant.ofEpochSecond(1), model.state.value.lastAttempt)
    }

    @Test fun firstFailureCreatesNoSuccessfulObservation() = runTest(dispatcher) {
        val model = model(); source.spaces[MAINFRAME] = SpaceStateException.Timeout()
        model.refresh(); runCurrent()
        assertNull(model.state.value.spaceTiming)
        assertNull(model.state.value.lastUpdate)
        assertEquals(Instant.EPOCH, model.state.value.lastAttempt)
    }

    @Test fun missingEvidenceNeverCountsAsCurrentOpening() = runTest(dispatcher) {
        val model = model()
        source.spaceObservations[MAINFRAME] = StatusObservation(space(MAINFRAME, "A"), timing(observed = null, expiry = null))
        model.refresh(); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(ObservationFreshness.UNKNOWN, model.state.value.freshness)
        assertNull(model.state.value.error)
    }

    @Test fun selectionResetClearsAllEvidenceAndErrors() = runTest(dispatcher) {
        val model = model(); model.refresh(); runCurrent()
        model.selectSpace(LEINELAB); runCurrent()
        assertNull(model.state.value.info)
        assertNull(model.state.value.spaceTiming)
        assertNull(model.state.value.roomsTiming)
        assertNull(model.state.value.lastUpdate)
        assertNull(model.state.value.lastAttempt)
        assertNull(model.state.value.roomsError)
        assertTrue(model.state.value.rooms.isEmpty())
        advanceTimeBy(900_000); runCurrent()
        assertEquals(LEINELAB, model.state.value.endpoint)
    }

    @Test fun returnChecksExpiredDataBeforeWaitingForANewRead() = runTest(dispatcher) {
        val model = model(); model.refresh(); runCurrent()
        advanceTimeBy(900_000); runCurrent()
        source.gates[MAINFRAME] = CompletableDeferred()
        val polling = launch { model.poll() }; runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(Instant.EPOCH, model.state.value.spaceTiming?.observedAt)
        polling.cancel()
    }

    @Test fun repeatedManualRefreshesShareOnePendingAttempt() = runTest(dispatcher) {
        val model = model(); source.gates[MAINFRAME] = CompletableDeferred()
        model.refresh(); model.refresh(); runCurrent()
        assertEquals(1, source.spaceReads.size)
        assertTrue(model.state.value.isRefreshing)
        source.gates[MAINFRAME]!!.complete(Unit); runCurrent()
        assertFalse(model.state.value.isRefreshing)
        assertEquals(1, source.spaceReads.size)
    }

    @Test fun lateReadCannotRepopulateAnEndpointSelectedAgain() = runTest(dispatcher) {
        val model = model()
        val gate = CompletableDeferred<Unit>()
        source.ignoresCancellation = true
        source.gates[MAINFRAME] = gate
        model.refresh(); runCurrent()
        model.selectSpace(LEINELAB); runCurrent()
        model.selectSpace(MAINFRAME); runCurrent()
        assertNull(model.state.value.info)
        source.gates.remove(MAINFRAME)
        source.spaces[MAINFRAME] = space(MAINFRAME, "New")
        model.refresh(); runCurrent()
        assertTrue(model.state.value.isRefreshing)
        gate.complete(Unit); runCurrent()
        assertEquals("New", model.state.value.info?.name)
        assertFalse(model.state.value.isRefreshing)
    }

    @Test fun clockRollbackIsUnknownWithoutChangingOriginalEvidence() = runTest(dispatcher) {
        var clock = Instant.ofEpochSecond(100)
        source.now = { clock }
        source.spaces[MAINFRAME] = space(MAINFRAME, "A")
        val model = StatusViewModel(source, settings, source.now); runCurrent()
        model.refresh(); runCurrent()
        val original = model.state.value.spaceTiming
        clock = Instant.ofEpochSecond(99)
        model.recheck(); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
        assertEquals(ObservationFreshness.UNKNOWN, model.state.value.freshness)
        assertEquals(original, model.state.value.spaceTiming)
    }

}
