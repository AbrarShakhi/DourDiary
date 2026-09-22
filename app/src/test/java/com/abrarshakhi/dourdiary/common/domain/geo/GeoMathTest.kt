package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    private val oneDegreeMeters = 111_195.08

    private val halfCircumferenceMeters = 20_015_114.4

    @Test
    fun `a point is zero distance from itself`() {
        val point = GeoPoint(52.5200, 13.4050)

        assertEquals(0.0, GeoMath.distanceBetween(point, point).meters, 1e-9)
    }

    @Test
    fun `one degree of longitude along the equator is one degree of arc`() {
        val distance = GeoMath.distanceBetween(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))

        assertEquals(oneDegreeMeters, distance.meters, 0.1)
    }

    @Test
    fun `one degree of latitude is one degree of arc at any longitude`() {
        val atPrimeMeridian = GeoMath.distanceBetween(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0))
        val farEast = GeoMath.distanceBetween(GeoPoint(40.0, 130.0), GeoPoint(41.0, 130.0))

        assertEquals(oneDegreeMeters, atPrimeMeridian.meters, 0.1)
        assertEquals(oneDegreeMeters, farEast.meters, 0.1)
    }

    @Test
    fun `longitude lines converge towards the poles`() {
        val atEquator = GeoMath.distanceBetween(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))
        val atSixtyNorth = GeoMath.distanceBetween(GeoPoint(60.0, 0.0), GeoPoint(60.0, 1.0))

        assertEquals(atEquator.meters / 2.0, atSixtyNorth.meters, 1.0)
    }

    @Test
    fun `pole to pole is half the circumference`() {
        val distance = GeoMath.distanceBetween(GeoPoint(90.0, 0.0), GeoPoint(-90.0, 0.0))

        assertEquals(halfCircumferenceMeters, distance.meters, 1.0)
    }

    @Test
    fun `antipodal points do not overflow the arcsine`() {
        val distance = GeoMath.distanceBetween(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0))

        assertTrue("Antipodal distance was not finite: $distance", distance.meters.isFinite())
        assertEquals(halfCircumferenceMeters, distance.meters, 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeoPoint(48.8584, 2.2945)
        val b = GeoPoint(51.5007, -0.1246)

        assertEquals(
            GeoMath.distanceBetween(a, b).meters,
            GeoMath.distanceBetween(b, a).meters,
            1e-6,
        )
    }

    @Test
    fun `short legs stay accurate`() {
        val start = GeoPoint(52.5200, 13.4050)
        val tenMetresNorth = GeoPoint(52.5200 + 10.0 / oneDegreeMeters, 13.4050)

        assertEquals(10.0, GeoMath.distanceBetween(start, tenMetresNorth).meters, 0.01)
    }

    @Test
    fun `crossing the antimeridian is a short hop not a trip round the world`() {
        val west = GeoPoint(0.0, 179.999)
        val east = GeoPoint(0.0, -179.999)

        val distance = GeoMath.distanceBetween(west, east)

        assertEquals(oneDegreeMeters * 0.002, distance.meters, 0.5)
    }
}
