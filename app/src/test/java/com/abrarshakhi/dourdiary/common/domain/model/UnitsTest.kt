package com.abrarshakhi.dourdiary.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceTest {

    @Test
    fun `converts between metres kilometres and miles`() {
        val distance = Distance(1_609.344)

        assertEquals(1.609344, distance.inKilometers, 1e-9)
        assertEquals(1.0, distance.inMiles, 1e-9)
    }

    @Test
    fun `subtraction never goes negative`() {
        assertEquals(Distance.Zero, Distance(5.0) - Distance(9.0))
    }

    @Test
    fun `rejects negative and NaN`() {
        assertThrows(IllegalArgumentException::class.java) { Distance(-1.0) }
        assertThrows(IllegalArgumentException::class.java) { Distance(Double.NaN) }
    }

    @Test
    fun `orders by magnitude`() {
        assertTrue(Distance.ofKilometers(5.0) > Distance.ofKilometers(1.0))
        assertTrue(Distance.ofMiles(1.0) > Distance.ofKilometers(1.0))
    }

    @Test
    fun `reads in the user's unit`() {
        val fiveKm = Distance.ofKilometers(5.0)

        assertEquals(5.0, fiveKm.inUnit(UnitSystem.METRIC), 1e-9)
        assertEquals(3.1069, fiveKm.inUnit(UnitSystem.IMPERIAL), 1e-4)
    }
}

class PaceTest {

    @Test
    fun `pace is the inverse of speed`() {
        val pace = Pace.fromSpeed(Speed(10.0 / 3.6))

        assertEquals(360.0, pace!!.secondsPerKilometer, 1e-6)
    }

    @Test
    fun `standing still has no pace rather than an infinite one`() {
        assertNull(Pace.fromSpeed(Speed.Zero))
        assertNull(Pace.of(Distance.Zero, 60_000L))
        assertNull(Pace.of(Distance.ofKilometers(1.0), 0L))
    }

    @Test
    fun `derives pace from distance and duration`() {
        val pace = Pace.of(Distance.ofKilometers(5.0), durationMillis = 25 * 60 * 1_000L)

        assertEquals(300.0, pace!!.secondsPerKilometer, 1e-6)
    }

    @Test
    fun `converts to seconds per mile`() {
        val pace = Pace(300.0)

        assertEquals(482.8032, pace.secondsPerMile, 1e-4)
    }

    @Test
    fun `a run pace is withheld until the distance means something`() {
        assertNull(Pace.ofRun(Distance(3.0), durationMillis = 7_000L))
        assertNull(Pace.ofRun(Distance(49.9), durationMillis = 30_000L))
    }

    @Test
    fun `a run pace appears once the distance is real`() {
        val pace = Pace.ofRun(Distance(50.0), durationMillis = 15_000L)

        assertEquals(300.0, pace!!.secondsPerKilometer, 1e-6)
    }

    @Test
    fun `a faster pace sorts before a slower one`() {
        assertTrue(Pace(240.0) < Pace(300.0))
    }
}

class SpeedTest {

    @Test
    fun `derives speed from distance and duration`() {
        val speed = Speed.of(Distance(100.0), durationMillis = 10_000L)

        assertEquals(10.0, speed!!.metersPerSecond, 1e-9)
        assertEquals(36.0, speed.kilometersPerHour, 1e-9)
    }

    @Test
    fun `no time means no speed`() {
        assertNull(Speed.of(Distance(100.0), durationMillis = 0L))
    }
}

class UnitSystemTest {

    @Test
    fun `cue unit is one kilometre or one mile`() {
        assertEquals(1_000.0, UnitSystem.METRIC.cueUnit.meters, 1e-9)
        assertEquals(1_609.344, UnitSystem.IMPERIAL.cueUnit.meters, 1e-9)
    }
}
