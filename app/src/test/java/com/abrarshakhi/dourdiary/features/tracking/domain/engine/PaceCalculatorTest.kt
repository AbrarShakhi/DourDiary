package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceCalculatorTest {
    private fun steadySamples(
        metersPerSecond: Double,
        seconds: Int,
        fromMillis: Long = 0L,
    ): List<PaceSample> = (0..seconds).map { second ->
        PaceSample(
            elapsedRealtimeMillis = fromMillis + second * 1_000L,
            cumulativeDistanceMeters = second * metersPerSecond,
        )
    }

    @Test
    fun `no pace before two samples exist`() {
        assertNull(PaceCalculator.rollingPace(emptyList(), 30_000L))
        assertNull(PaceCalculator.rollingPace(listOf(PaceSample(0L, 0.0)), 30_000L))
    }

    @Test
    fun `steady running gives the matching pace`() {
        val samples = steadySamples(metersPerSecond = 3.0, seconds = 120)

        val pace = PaceCalculator.rollingPace(samples, windowMillis = 30_000L)

        assertEquals(1_000.0 / 3.0, pace!!.secondsPerKilometer, 0.01)
    }

    @Test
    fun `the window only reflects recent running`() {
        val slow = steadySamples(metersPerSecond = 2.0, seconds = 90)
        val fastStart = slow.last()
        val fast = (1..30).map { second ->
            PaceSample(
                elapsedRealtimeMillis = fastStart.elapsedRealtimeMillis + second * 1_000L,
                cumulativeDistanceMeters = fastStart.cumulativeDistanceMeters + second * 5.0,
            )
        }

        val pace = PaceCalculator.rollingPace(slow + fast, windowMillis = 30_000L)

        assertEquals(200.0, pace!!.secondsPerKilometer, 1.0)
    }

    @Test
    fun `a run younger than the window uses everything it has`() {
        val samples = steadySamples(metersPerSecond = 4.0, seconds = 5)

        val pace = PaceCalculator.rollingPace(samples, windowMillis = 30_000L)

        assertEquals(250.0, pace!!.secondsPerKilometer, 0.01)
    }

    @Test
    fun `standing still within the window has no pace`() {
        val stationary = (0..40).map { second ->
            PaceSample(elapsedRealtimeMillis = second * 1_000L, cumulativeDistanceMeters = 100.0)
        }

        assertNull(PaceCalculator.rollingPace(stationary, windowMillis = 30_000L))
    }

    @Test
    fun `a window covering too little ground reports no pace`() {
        val drifting = steadySamples(metersPerSecond = 0.5, seconds = 10)

        assertNull(
            PaceCalculator.rollingPace(drifting, windowMillis = 30_000L, minimumDistanceMeters = 20.0),
        )
    }

    @Test
    fun `a window covering real ground still reports pace`() {
        val running = steadySamples(metersPerSecond = 3.0, seconds = 30)

        assertNotNull(
            PaceCalculator.rollingPace(running, windowMillis = 30_000L, minimumDistanceMeters = 20.0),
        )
    }

    @Test
    fun `average pace is withheld for a distance dominated by noise`() {
        assertNull(PaceCalculator.averagePace(Distance(3.0), movingDurationMillis = 7_000L))
    }

    @Test
    fun `average pace spans the whole run`() {
        val pace = PaceCalculator.averagePace(
            distance = Distance.ofKilometers(10.0),
            movingDurationMillis = 50 * 60 * 1_000L,
        )

        assertEquals(300.0, pace!!.secondsPerKilometer, 1e-6)
    }

    @Test
    fun `pruning keeps enough history to span the window`() {
        val samples = steadySamples(metersPerSecond = 3.0, seconds = 300)

        val pruned = PaceCalculator.prune(samples, windowMillis = 30_000L)

        assertTrue("Pruning did not bound the list: ${pruned.size}", pruned.size < samples.size)
        assertEquals(
            PaceCalculator.rollingPace(samples, 30_000L)!!.secondsPerKilometer,
            PaceCalculator.rollingPace(pruned, 30_000L)!!.secondsPerKilometer,
            1e-9,
        )
    }

    @Test
    fun `pruning a short list leaves it alone`() {
        val samples = steadySamples(metersPerSecond = 3.0, seconds = 5)

        assertEquals(samples, PaceCalculator.prune(samples, windowMillis = 30_000L))
    }
}
