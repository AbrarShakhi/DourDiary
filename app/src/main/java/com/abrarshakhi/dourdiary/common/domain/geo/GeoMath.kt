package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    const val EarthRadiusMeters = 6_371_008.8

    fun distanceBetween(start: GeoPoint, end: GeoPoint): Distance {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(end.longitude - start.longitude)

        val sinHalfLat = sin(deltaLat / 2.0)
        val sinHalfLon = sin(deltaLon / 2.0)

        val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLon * sinHalfLon
        val centralAngle = 2.0 * asin(min(1.0, sqrt(a)))

        return Distance(EarthRadiusMeters * centralAngle)
    }
}
