package com.abrarshakhi.dourdiary.common.presentation.map

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RouteProjectionTest {

    @Test
    fun `an empty route projects to nothing`() {
        assertEquals(emptyList<NormalizedPoint>(), RouteProjection.project(emptyList()))
    }

    @Test
    fun `a single point sits in the middle`() {
        val projected = RouteProjection.project(listOf(GeoPoint(52.52, 13.405)))

        assertEquals(1, projected.size)
        assertEquals(0.5f, projected.single().x, 1e-6f)
        assertEquals(0.5f, projected.single().y, 1e-6f)
    }

    @Test
    fun `a run that never moved does not divide by zero`() {
        val stationary = List(10) { GeoPoint(52.52, 13.405) }

        val projected = RouteProjection.project(stationary)

        assertTrue(projected.all { it.x == 0.5f && it.y == 0.5f })
    }

    @Test
    fun `everything lands inside the unit square`() {
        val route = (0..100).map { GeoPoint(52.52 + it * 0.0005, 13.405 + it * 0.0009) }

        val projected = RouteProjection.project(route)

        assertTrue(
            "A point escaped the box: ${projected.filterNot { it.x in 0f..1f && it.y in 0f..1f }}",
            projected.all { it.x in 0f..1f && it.y in 0f..1f },
        )
    }

    @Test
    fun `north is up`() {
        val southThenNorth = listOf(GeoPoint(52.50, 13.405), GeoPoint(52.54, 13.405))

        val projected = RouteProjection.project(southThenNorth)

        assertTrue(
            "The northern point should have the smaller y",
            projected[1].y < projected[0].y,
        )
    }

    @Test
    fun `east is right`() {
        val westThenEast = listOf(GeoPoint(52.52, 13.40), GeoPoint(52.52, 13.44))

        val projected = RouteProjection.project(westThenEast)

        assertTrue(projected[1].x > projected[0].x)
    }

    @Test
    fun `shape is preserved rather than stretched to fill the box`() {
        val meanLatitude = 52.52
        val longitudeScale = kotlin.math.cos(Math.toRadians(meanLatitude))
        val widthDegrees = 0.02 / longitudeScale
        val heightDegrees = 0.01

        val corners = listOf(
            GeoPoint(meanLatitude, 13.40),
            GeoPoint(meanLatitude, 13.40 + widthDegrees),
            GeoPoint(meanLatitude + heightDegrees, 13.40 + widthDegrees),
            GeoPoint(meanLatitude + heightDegrees, 13.40),
        )

        val projected = RouteProjection.project(corners)
        val spanX = projected.maxOf { it.x } - projected.minOf { it.x }
        val spanY = projected.maxOf { it.y } - projected.minOf { it.y }

        assertEquals("Aspect ratio was not preserved", 2.0f, spanX / spanY, 0.02f)
    }

    @Test
    fun `the route is centred in the box`() {
        val route = (0..50).map { GeoPoint(52.52 + it * 0.0005, 13.405) }

        val projected = RouteProjection.project(route)
        val midX = (projected.maxOf { it.x } + projected.minOf { it.x }) / 2f
        val midY = (projected.maxOf { it.y } + projected.minOf { it.y }) / 2f

        assertTrue(abs(midX - 0.5f) < 1e-5f)
        assertTrue(abs(midY - 0.5f) < 1e-5f)
    }

    @Test
    fun `longitude is scaled so a far northern route is not squashed`() {
        val north = listOf(GeoPoint(70.0, 10.0), GeoPoint(70.0, 10.1), GeoPoint(70.05, 10.0))
        val equator = listOf(GeoPoint(0.0, 10.0), GeoPoint(0.0, 10.1), GeoPoint(0.05, 10.0))

        val northSpan = RouteProjection.project(north).let { p ->
            (p.maxOf { it.x } - p.minOf { it.x }) / (p.maxOf { it.y } - p.minOf { it.y })
        }
        val equatorSpan = RouteProjection.project(equator).let { p ->
            (p.maxOf { it.x } - p.minOf { it.x }) / (p.maxOf { it.y } - p.minOf { it.y })
        }

        assertTrue(
            "The northern route was not narrowed relative to the equatorial one",
            northSpan < equatorSpan,
        )
    }

    @Test
    fun `extent is only reported once the route actually moves`() {
        assertFalse(RouteProjection.hasExtent(emptyList()))
        assertFalse(RouteProjection.hasExtent(listOf(GeoPoint(52.52, 13.405))))
        assertFalse(RouteProjection.hasExtent(List(5) { GeoPoint(52.52, 13.405) }))
        assertTrue(
            RouteProjection.hasExtent(listOf(GeoPoint(52.52, 13.405), GeoPoint(52.53, 13.405))),
        )
    }
}
