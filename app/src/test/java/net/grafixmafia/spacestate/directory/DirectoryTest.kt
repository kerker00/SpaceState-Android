package net.grafixmafia.spacestate.directory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.grafixmafia.spacestate.FakeSource
import net.grafixmafia.spacestatekit.DirectoryEntry
import net.grafixmafia.spacestatekit.SpaceStateException
import net.grafixmafia.spacestatekit.SpaceStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryTest {
    private val dispatcher = StandardTestDispatcher()

    private val entries = listOf(
        DirectoryEntry("https://a.example/", "Mainframe", "Bahnhofsplatz 10, 26122 Oldenburg, Germany", SpaceStatus.OPEN),
        DirectoryEntry("https://b.example/", "Hackspace Köln", null, SpaceStatus.CLOSED),
        DirectoryEntry("https://c.example/", "LeineLab", "Hannover", SpaceStatus.UNKNOWN),
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun blankQueryKeepsEverything() {
        assertEquals(entries, entries.matching("  "))
    }

    @Test fun matchesNameAndAddressIgnoringCaseAndAccents() {
        assertEquals(listOf("Mainframe"), entries.matching("oldenb").map { it.name })
        assertEquals(listOf("Hackspace Köln"), entries.matching("KOLN").map { it.name })
        assertEquals(listOf("LeineLab"), entries.matching(" leine ").map { it.name })
    }

    @Test fun loadsOnceAndRetriesAfterFailure() = runTest(dispatcher) {
        val source = FakeSource().apply { directory = Result.failure(SpaceStateException.Unreachable()) }
        val model = DirectoryViewModel(source)

        model.loadIfNeeded()
        advanceUntilIdle()
        assertTrue(model.state.value.error is SpaceStateException.Unreachable)

        source.directory = Result.success(entries)
        model.load()
        advanceUntilIdle()
        model.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(entries, model.state.value.entries)
        assertEquals(2, source.directoryReads)
    }
}
