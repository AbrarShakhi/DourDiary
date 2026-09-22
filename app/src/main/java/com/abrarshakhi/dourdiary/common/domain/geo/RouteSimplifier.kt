package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

object RouteSimplifier {
    const val DefaultToleranceMeters = 8.0
    const val DefaultMaxPoints = 120

    fun simplify(
        points: List<GeoPoint>,
        toleranceMeters: Double = DefaultToleranceMeters,
        maxPoints: Int = DefaultMaxPoints,
    ): List<GeoPoint> {
        if (points.size <= 2) return points

        val kept = BooleanArray(points.size)
        kept[0] = true
        kept[points.lastIndex] = true

        val metersPerDegreeLatitude = GeoMath.EarthRadiusMeters * Math.PI / 180.0
        val meanLatitude = points.sumOf { it.latitude } / points.size
        val metersPerDegreeLongitude = metersPerDegreeLatitude * cos(Math.toRadians(meanLatitude))

        fun x(point: GeoPoint) = point.longitude * metersPerDegreeLongitude
        fun y(point: GeoPoint) = point.latitude * metersPerDegreeLatitude

        val pending = ArrayDeque<Pair<Int, Int>>()
        pending += 0 to points.lastIndex

        while (pending.isNotEmpty()) {
            val (first, last) = pending.removeFirst()
            if (last <= first + 1) continue

            val startX = x(points[first])
            val startY = y(points[first])
            val endX = x(points[last])
            val endY = y(points[last])
            val segmentLength = hypot(endX - startX, endY - startY)

            var farthestIndex = -1
            var farthestDistance = 0.0

            for (index in first + 1 until last) {
                val pointX = x(points[index])
                val pointY = y(points[index])
                val distance = if (segmentLength == 0.0) {
                    hypot(pointX - startX, pointY - startY)
                } else {
                    abs(
                        (endY - startY) * pointX - (endX - startX) * pointY +
                            endX * startY - endY * startX,
                    ) / segmentLength
                }
                if (distance > farthestDistance) {
                    farthestDistance = distance
                    farthestIndex = index
                }
            }

            if (farthestIndex >= 0 && farthestDistance > toleranceMeters) {
                kept[farthestIndex] = true
                pending += first to farthestIndex
                pending += farthestIndex to last
            }
        }

        val simplified = points.filterIndexed { index, _ -> kept[index] }
        return sample(simplified, maxPoints)
    }

    fun sample(points: List<GeoPoint>, maxPoints: Int): List<GeoPoint> {
        require(maxPoints >= 2) { "maxPoints must allow both ends, was $maxPoints" }
        if (points.size <= maxPoints) return points
        val step = (points.size - 1).toDouble() / (maxPoints - 1)
        val sampled = (0 until maxPoints).map { points[(it * step).toInt()] }
        return sampled.dropLast(1) + points.last()
    }
}
