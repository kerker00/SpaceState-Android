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
import kotlinx.coroutines.test.setMain
import net.grafixmafia.spacestate.FakeSettings
import net.grafixmafia.spacestate.FakeSource
import net.grafixmafia.spacestate.TestViewModels
import net.grafixmafia.spacestate.LEINELAB
import net.grafixmafia.spacestate.MAINFRAME
import net.grafixmafia.spacestate.room
import net.grafixmafia.spacestate.space
import net.grafixmafia.spacestatekit.MainframeRoomState
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatusViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = TestViewModels()
    private val source = FakeSource()
    private val settings = FakeSettings()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    /** A model that has read the settings, as it has by the time the user can act. */
    private fun TestScope.model(): StatusViewModel {
        source.now = { Instant.ofEpochMilli(testScheduler.currentTime) }
        return models.track(StatusViewModel(source, settings, now = source.now)).also { runCurrent() }
    }

    @Test fun showsSpaceWithMainframeRooms() = models.run(dispatcher) {
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        source.rooms = Result.success(listOf(room("space", MainframeRoomState.OPEN_PLUS)))
        val model = model()

        model.refresh()
        runCurrent()

        val state = model.state.value
        assertEquals("Mainframe", state.info?.name)
        assertEquals(SpaceStatus.OPEN, state.status)
        assertEquals(listOf("space"), state.rooms.map { it.id })
        assertFalse(state.isRefreshing)
    }

    @Test fun failingRoomsDoNotHideTheSpace() = models.run(dispatcher) {
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        source.rooms = Result.failure(SpaceStateException.Timeout())
        val model = model()

        model.refresh()
        runCurrent()

        assertEquals(SpaceStatus.OPEN, model.state.value.status)
        assertTrue(model.state.value.rooms.isEmpty())
        assertNull(model.state.value.error)
    }

    @Test fun otherSpacesLoadNoRooms() = models.run(dispatcher) {
        settings.settings.value = settings.settings.value.copy(endpoint = LEINELAB)
        source.spaces[LEINELAB] = space(LEINELAB, "LeineLab")
        val model = model()

        model.refresh()
        runCurrent()

        assertEquals("LeineLab", model.state.value.info?.name)
        assertEquals(0, source.roomReads)
    }

    @Test fun failureShowsUnknownButKeepsLastInfo() = models.run(dispatcher) {
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        val model = model()
        model.refresh()
        runCurrent()

        source.spaces[MAINFRAME] = SpaceStateException.Unreachable()
        model.refresh()
        runCurrent()

        val state = model.state.value
        assertEquals(SpaceStatus.UNKNOWN, state.status)
        assertEquals("Mainframe", state.info?.name)
        assertTrue(state.error is SpaceStateException.Unreachable)
    }

    @Test fun choosingAnotherSpaceClearsTheOldOne() = models.run(dispatcher) {
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        val model = model()
        model.refresh()
        runCurrent()

        model.selectSpace(LEINELAB)
        runCurrent()

        assertEquals(LEINELAB, model.state.value.endpoint)
        assertNull(model.state.value.info)
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.status)
    }

    @Test fun lateAnswerForThePreviousSpaceIsDropped() = models.run(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        source.gates[MAINFRAME] = gate
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        val model = model()
        model.refresh()
        runCurrent()

        model.selectSpace(LEINELAB)
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertEquals(LEINELAB, model.state.value.endpoint)
        assertNull(model.state.value.info)
    }

    @Test fun pollingRefreshesEveryInterval() = models.run(dispatcher) {
        source.spaces[MAINFRAME] = space(MAINFRAME, "Mainframe")
        settings.settings.value = settings.settings.value.copy(refreshSeconds = 60)
        val model = model()

        val polling = launch { model.poll() }
        runCurrent()
        assertEquals(1, source.spaceReads.size)
        advanceTimeBy(60_001)
        assertEquals(2, source.spaceReads.size)

        // A new interval starts over with an immediate read.
        model.setRefreshSeconds(300)
        runCurrent()
        assertEquals(3, source.spaceReads.size)
        advanceTimeBy(120_000)
        assertEquals(3, source.spaceReads.size)
        polling.cancel()
    }
}
