package net.grafixmafia.spacestate.directory

import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.grafixmafia.spacestate.FakeSource
import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.ObservationSource
import net.grafixmafia.spacestatekit.StatusObservation
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryObservationTest {
    private val dispatcher = StandardTestDispatcher()
    private val source = FakeSource()
    private val entry = DirectoryEntry("https://a.example/api", "A", "Oldenburg", SpaceStatus.OPEN)
    private fun timing(observed: Long? = 0, received: Long = 0, expiry: Long? = 900) = ObservationTiming(
        ObservationSource.AGGREGATOR, observed?.let(Instant::ofEpochSecond), Instant.ofEpochSecond(received), expiry?.let(Instant::ofEpochSecond))
    private fun TestScope.model(): DirectoryViewModel {
        source.now = { Instant.ofEpochMilli(testScheduler.currentTime) }
        source.directory = Result.success(listOf(entry))
        source.directoryObservations = listOf(StatusObservation(entry, timing()))
        return DirectoryViewModel(source, source.now)
    }
    @Before fun start() = Dispatchers.setMain(dispatcher)
    @After fun stop() = Dispatchers.resetMain()

    @Test fun directoryExpiresWithoutDownloadingOrRestampingItsEvidence() = runTest(dispatcher) {
        val model = model(); source.directoryObservations = listOf(StatusObservation(entry, timing(expiry = 2)))
        model.load(); runCurrent()
        val original = model.state.value
        advanceTimeBy(2000); runCurrent()
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.displayEntries.single().status)
        assertEquals(original.timings, model.state.value.timings)
        assertEquals(original.lastAttempt, model.state.value.lastAttempt)
        assertEquals(original.lastUpdate, model.state.value.lastUpdate)
        assertEquals(1, source.directoryReads)
    }

    @Test fun currentMissingFutureStaleAndUnreachableEntriesAreIndependent() = runTest(dispatcher) {
        advanceTimeBy(10_000); runCurrent()
        val model = model()
        val names = listOf("Current", "Missing", "Future", "Stale", "Unreachable")
        val times = listOf(timing(5, 10, 20), timing(null, 10, null), timing(11, 10, 21), timing(1, 10, 2), timing(5, 10, null))
        source.directoryObservations = names.zip(times).map { (name, evidence) ->
            StatusObservation(entry.copy(endpoint = "https://${name.lowercase()}.example/api", name = name), evidence)
        }
        model.load(); runCurrent()
        assertEquals(listOf(SpaceStatus.OPEN, SpaceStatus.UNKNOWN, SpaceStatus.UNKNOWN, SpaceStatus.UNKNOWN, SpaceStatus.UNKNOWN),
            model.state.value.displayEntries.map { it.status })
        assertEquals(names, model.state.value.entries.map { it.name })
    }

    @Test fun failedRefreshKeepsSearchableNamesWithoutCurrentFlags() = runTest(dispatcher) {
        val model = model(); model.load(); runCurrent()
        val original = model.state.value
        advanceTimeBy(1000); runCurrent()
        source.directory = Result.failure(SpaceStateException.Unreachable())
        model.load(); runCurrent()
        assertEquals(original.entries, model.state.value.entries)
        assertEquals(original.timings, model.state.value.timings)
        assertEquals(original.lastUpdate, model.state.value.lastUpdate)
        assertEquals(Instant.ofEpochSecond(1), model.state.value.lastAttempt)
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.displayEntries.single().status)
        assertEquals("A", model.state.value.displayEntries.matching("olden").single().name)
    }

    @Test fun cancellationDoesNotCreateFailureOrSuccessfulReceipt() = runTest(dispatcher) {
        val model = model(); model.load(); runCurrent()
        val original = model.state.value
        advanceTimeBy(1000); runCurrent()
        source.directory = Result.failure(CancellationException())
        model.load(); runCurrent()
        assertEquals(original.lastUpdate, model.state.value.lastUpdate)
        assertEquals(original.timings, model.state.value.timings)
        assertNull(model.state.value.error)
        assertFalse(model.state.value.isLoading)
    }

    @Test fun cachedNamesOnReturnDoNotExtendTheirStates() = runTest(dispatcher) {
        val model = model(); source.directoryObservations = listOf(StatusObservation(entry, timing(expiry = 2)))
        model.load(); runCurrent()
        advanceTimeBy(3000); runCurrent()
        model.loadIfNeeded(); runCurrent()
        assertEquals(1, source.directoryReads)
        assertEquals(SpaceStatus.UNKNOWN, model.state.value.displayEntries.single().status)
        assertEquals(Instant.EPOCH, model.state.value.timings[entry.endpoint]?.observedAt)
    }

    @Test fun repeatedLoadRequestsDoNotOverlap() = runTest(dispatcher) {
        val model = model(); model.load(); model.load(); runCurrent()
        assertEquals(1, source.directoryReads)
        assertFalse(model.state.value.isLoading)
    }

    @Test fun firstFailureProvidesAttemptWithoutSourceOrSuccessTime() = runTest(dispatcher) {
        val model = model(); source.directory = Result.failure(SpaceStateException.Timeout())
        model.load(); runCurrent()
        assertNull(model.state.value.lastUpdate)
        assertEquals(Instant.EPOCH, model.state.value.lastAttempt)
        assertTrue(model.state.value.timings.isEmpty())
    }
}
