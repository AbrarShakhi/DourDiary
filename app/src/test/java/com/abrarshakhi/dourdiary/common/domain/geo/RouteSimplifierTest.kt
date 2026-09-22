package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSimplifierTest {

    private val metersPerDegree = 111_195.08

    private fun straightLine(points: Int, spacingMeters: Double = 5.0) =
        (0 until points).map { GeoPoint(52.52 + it * spacingMeters / metersPerDegree, 13.405) }

    @Test
    fun `a route too short to simplify is returned untouched`() {
        val two = straightLine(2)

        assertEquals(two, RouteSimplifier.simplify(two))
    }

    @Test
    fun `a straight line collapses to its endpoints`() {
        val line = straightLine(200)

        val simplified = RouteSimplifier.simplify(line)

        assertEquals(2, simplified.size)
        assertEquals(line.first(), simplified.first())
        assertEquals(line.last(), simplified.last())
    }

    @Test
    fun `a corner is kept because it defines the shape`() {
        val north = (0..50).map { GeoPoint(52.52 + it * 5.0 / metersPerDegree, 13.405) }
        val corner = north.last()
        val east = (1..50).map { GeoPoint(corner.latitude, 13.405 + it * 5.0 / metersPerDegree) }

        val simplified = RouteSimplifier.simplify(north + east)

        assertEquals("Expected start, corner and end", 3, simplified.size)
        assertEquals(corner.latitude, simplified[1].latitude, 1e-9)
    }

    @Test
    fun `endpoints always survive`() {
        val route = (0..500).map {
            GeoPoint(52.52 + it * 0.00005, 13.405 + kotlin.math.sin(it / 20.0) * 0.001)
        }

        val simplified = RouteSimplifier.simplify(route)

        assertEquals(route.first(), simplified.first())
        assertEquals(route.last(), simplified.last())
    }

    @Test
    fun `a wandering route is bounded by the point cap`() {
        val zigzag = (0..2_000).map { index ->
            GeoPoint(
                latitude = 52.52 + index * 0.0002,
                longitude = 13.405 + if (index % 2 == 0) 0.0008 else -0.0008,
            )
        }

        val simplified = RouteSimplifier.simplify(zigzag)

        assertTrue(
            "Cap not honoured: ${simplified.size}",
            simplified.size <= RouteSimplifier.DefaultMaxPoints,
        )
        assertEquals(zigzag.last(), simplified.last())
    }

    @Test
    fun `a very long route does not overflow the stack`() {
        val long = (0..50_000).map {
            GeoPoint(52.52 + it * 0.000002, 13.405 + kotlin.math.sin(it / 500.0) * 0.002)
        }

        val simplified = RouteSimplifier.simplify(long)

        assertTrue(simplified.size in 2..RouteSimplifier.DefaultMaxPoints)
    }

    @Test
    fun `sampling bounds the point count whatever the input`() {
        listOf(10, 1_000, 20_000, 200_000).forEach { size ->
            val route = (0 until size).map { GeoPoint(52.52 + it * 1e-6, 13.405 + it * 1e-6) }

            val sampled = RouteSimplifier.sample(route, maxPoints = 1_000)

            assertTrue("size=${'$'}size gave ${'$'}{sampled.size}", sampled.size <= 1_000)
        }
    }

    @Test
    fun `sampling keeps both ends exactly`() {
        val route = (0..5_000).map { GeoPoint(52.52 + it * 1e-5, 13.405) }

        val sampled = RouteSimplifier.sample(route, maxPoints = 100)

        assertEquals(route.first(), sampled.first())
        assertEquals(route.last(), sampled.last())
    }

    @Test
    fun `sampling leaves a short route untouched`() {
        val route = (0..20).map { GeoPoint(52.52 + it * 1e-5, 13.405) }

        assertEquals(route, RouteSimplifier.sample(route, maxPoints = 1_000))
    }

    @Test
    fun `sampling preserves the order it was given`() {
        val route = (0..900).map { GeoPoint(52.52 + it * 1e-5, 13.405) }

        val sampled = RouteSimplifier.sample(route, maxPoints = 50)

        sampled.zipWithNext { earlier, later ->
            assertTrue("Sampling reordered the route", later.latitude > earlier.latitude)
        }
    }

    @Test
    fun `sampling refuses a cap that cannot hold both ends`() {
        val route = (0..100).map { GeoPoint(52.52 + it * 1e-5, 13.405) }

        assertThrows(IllegalArgumentException::class.java) {
            RouteSimplifier.sample(route, maxPoints = 1)
        }
    }

    @Test
    fun `a tighter tolerance keeps more detail`() {
        val wavy = (0..400).map {
            GeoPoint(52.52 + it * 0.00003, 13.405 + kotlin.math.sin(it / 8.0) * 0.0002)
        }

        val coarse = RouteSimplifier.simplify(wavy, toleranceMeters = 30.0)
        val fine = RouteSimplifier.simplify(wavy, toleranceMeters = 2.0)

        assertTrue("coarse=${coarse.size} fine=${fine.size}", fine.size > coarse.size)
    }
}
