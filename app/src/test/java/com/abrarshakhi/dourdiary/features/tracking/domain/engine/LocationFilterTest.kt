package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFilterTest {

    private val config = TrackingConfig(
        maxAccuracyMeters = 25.0,
        minDisplacementMeters = 3.0,
        maxPlausibleSpeedMetersPerSecond = 12.0,
    )
    private val filter = LocationFilter(config)

    private val metersPerDegreeLatitude = 111_195.08
    private val origin = GeoPoint(52.5200, 13.4050)

    private fun previousAt(millis: Long) = TrackPoint(
        point = origin,
        elapsedRealtimeMillis = millis,
        epochMillis = 1_700_000_000_000L + millis,
        cumulativeDistance = Distance.Zero,
    )

    private fun sampleNorthOf(
        meters: Double,
        atMillis: Long,
        accuracyMeters: Double = 5.0,
    ) = LocationSample(
        point = GeoPoint(origin.latitude + meters / metersPerDegreeLatitude, origin.longitude),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = atMillis,
        epochMillis = 1_700_000_000_000L + atMillis,
    )

    @Test
    fun `the first fix anchors the route and adds no distance`() {
        val verdict = filter.evaluate(previous = null, candidate = sampleNorthOf(0.0, 0L))

        assertEquals(FilterVerdict.Accept(Distance.Zero), verdict)
    }

    @Test
    fun `rejects a fix that is too inaccurate`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(10.0, 1_000L, accuracyMeters = 40.0),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.POOR_ACCURACY), verdict)
    }

    @Test
    fun `accuracy is checked before anything else so the first fix is not trusted blindly`() {
        val verdict = filter.evaluate(
            previous = null,
            candidate = sampleNorthOf(0.0, 0L, accuracyMeters = 100.0),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.POOR_ACCURACY), verdict)
    }

    @Test
    fun `rejects a fix that arrives out of order`() {
        val verdict = filter.evaluate(
            previous = previousAt(10_000L),
            candidate = sampleNorthOf(10.0, 5_000L),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.OUT_OF_ORDER), verdict)
    }

    @Test
    fun `rejects a duplicate timestamp`() {
        val verdict = filter.evaluate(
            previous = previousAt(5_000L),
            candidate = sampleNorthOf(10.0, 5_000L),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.OUT_OF_ORDER), verdict)
    }

    @Test
    fun `rejects a GPS jump faster than any human runs`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(100.0, 1_000L),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.IMPLAUSIBLE_SPEED), verdict)
    }

    @Test
    fun `accepts a genuine sprint`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(10.0, 1_000L),
        )

        assertTrue(verdict is FilterVerdict.Accept)
    }

    @Test
    fun `rejects drift below the displacement floor`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(1.5, 1_000L),
        )

        assertEquals(FilterVerdict.Reject(RejectionReason.BELOW_MIN_DISPLACEMENT), verdict)
    }

    @Test
    fun `accepts movement at exactly the displacement floor`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(3.0, 1_000L),
        )

        assertTrue(verdict is FilterVerdict.Accept)
    }

    @Test
    fun `reports the leg distance it measured`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(7.5, 1_000L),
        )

        val accepted = verdict as FilterVerdict.Accept
        assertEquals(7.5, accepted.legDistance.meters, 0.01)
    }

    @Test
    fun `a long gap makes a large jump plausible again`() {
        val verdict = filter.evaluate(
            previous = previousAt(0L),
            candidate = sampleNorthOf(100.0, 60_000L),
        )

        assertTrue(verdict is FilterVerdict.Accept)
    }
}
