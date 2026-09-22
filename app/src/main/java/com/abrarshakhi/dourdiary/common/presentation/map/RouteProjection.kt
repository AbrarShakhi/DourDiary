package com.abrarshakhi.dourdiary.common.presentation.map

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

data class NormalizedPoint(val x: Float, val y: Float)

object RouteProjection {
    private const val Fill = 0.9f

    fun project(points: List<GeoPoint>): List<NormalizedPoint> {
        if (points.isEmpty()) return emptyList()

        val meanLatitude = points.sumOf { it.latitude } / points.size
        val longitudeScale = cos(Math.toRadians(meanLatitude))

        val xs = points.map { it.longitude * longitudeScale }
        val ys = points.map { it.latitude }

        val minX = xs.min()
        val maxX = xs.max()
        val minY = ys.min()
        val maxY = ys.max()

        val spanX = maxX - minX
        val spanY = maxY - minY
        val span = max(spanX, spanY)

        if (span <= 0.0 || !span.isFinite()) {
            return points.map { NormalizedPoint(0.5f, 0.5f) }
        }

        val scale = Fill / span
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0

        return points.map { point ->
            NormalizedPoint(
                x = (0.5 + (point.longitude * longitudeScale - centerX) * scale).toFloat(),
                y = (0.5 - (point.latitude - centerY) * scale).toFloat(),
            )
        }
    }

    fun hasExtent(points: List<GeoPoint>): Boolean {
        if (points.size < 2) return false
        val first = points.first()
        return points.any {
            abs(it.latitude - first.latitude) > 1e-9 || abs(it.longitude - first.longitude) > 1e-9
        }
    }
}
