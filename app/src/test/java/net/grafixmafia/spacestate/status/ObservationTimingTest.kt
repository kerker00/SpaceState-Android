package net.grafixmafia.spacestate.status

import java.time.Instant
import net.grafixmafia.spacestatekit.ObservationTiming
import net.grafixmafia.spacestatekit.ObservationSource
import net.grafixmafia.spacestatekit.ObservationFreshness
import net.grafixmafia.spacestatekit.SpaceStatus
import org.junit.Test
import org.junit.Assert.*

class ObservationTimingTest {
    private val date = Instant.ofEpochSecond(100)
    private fun timing(observed: Instant? = date, received: Instant? = date, expiry: Instant? = date.plusSeconds(1),
                       source: ObservationSource = ObservationSource.SPACE_PUSH) = ObservationTiming(source, observed, received, expiry)

    @Test fun expiryIsExclusiveAndDoesNotKeepAnOpeningCurrent() {
        val value = timing()
        assertEquals(SpaceStatus.OPEN, value.currentStatus(SpaceStatus.OPEN, date))
        assertEquals(SpaceStatus.UNKNOWN, value.currentStatus(SpaceStatus.OPEN, date.plusSeconds(1)))
        assertEquals(ObservationFreshness.STALE, value.freshness(date.plusSeconds(1)))
        assertEquals(date, value.observedAt)
    }

    @Test fun missingReversedFutureAndUnknownSourceEvidenceNeverProvidesCurrentStatus() {
        for (value in listOf(timing(observed = null), timing(received = null), timing(expiry = null),
            timing(observed = date.plusSeconds(1)), timing(expiry = date.minusSeconds(1)), timing(source = ObservationSource.UNKNOWN))) {
            assertEquals(ObservationFreshness.UNKNOWN, value.freshness(date))
            assertEquals(SpaceStatus.UNKNOWN, value.currentStatus(SpaceStatus.OPEN, date))
        }
        assertEquals(ObservationFreshness.UNKNOWN, timing().freshness(date.minusSeconds(1)))
    }

    @Test fun aReportedUnknownStateIsNotTurnedIntoClosedByCurrentTiming() {
        assertEquals(SpaceStatus.UNKNOWN, timing().currentStatus(SpaceStatus.UNKNOWN, date))
        assertEquals(SpaceStatus.CLOSED, timing().currentStatus(SpaceStatus.CLOSED, date))
    }

    @Test fun fractionalExpiryIsPreservedInsteadOfRoundedToALaterSecond() {
        val expiry = date.plusNanos(500_000_000)
        val value = timing(expiry = expiry)
        assertEquals(ObservationFreshness.CURRENT, value.freshness(expiry.minusNanos(1)))
        assertEquals(ObservationFreshness.STALE, value.freshness(expiry))
    }
}
