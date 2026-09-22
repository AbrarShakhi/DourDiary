package com.abrarshakhi.dourdiary.common.domain.geo

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import kotlin.math.roundToInt

object PolylineCodec {

    private const val Precision = 1e5

    fun encode(points: List<GeoPoint>): String {
        val builder = StringBuilder()
        var previousLatitude = 0
        var previousLongitude = 0

        points.forEach { point ->
            val latitude = (point.latitude * Precision).roundToInt()
            val longitude = (point.longitude * Precision).roundToInt()
            builder.appendValue(latitude - previousLatitude)
            builder.appendValue(longitude - previousLongitude)
            previousLatitude = latitude
            previousLongitude = longitude
        }

        return builder.toString()
    }

    fun decode(encoded: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            val latitudeDelta = readValue(encoded, index) ?: break
            index = latitudeDelta.second
            val longitudeDelta = readValue(encoded, index) ?: break
            index = longitudeDelta.second

            latitude += latitudeDelta.first
            longitude += longitudeDelta.first

            val latitudeDegrees = latitude / Precision
            val longitudeDegrees = longitude / Precision
            if (latitudeDegrees !in -90.0..90.0 || longitudeDegrees !in -180.0..180.0) break
            points += GeoPoint(latitudeDegrees, longitudeDegrees)
        }

        return points
    }

    private fun StringBuilder.appendValue(value: Int) {
        var shifted = if (value < 0) (value shl 1).inv() else value shl 1
        while (shifted >= 0x20) {
            append(((0x20 or (shifted and 0x1f)) + 63).toChar())
            shifted = shifted shr 5
        }
        append((shifted + 63).toChar())
    }

    private fun readValue(encoded: String, startIndex: Int): Pair<Int, Int>? {
        var index = startIndex
        var shift = 0
        var result = 0
        var byte: Int

        do {
            if (index >= encoded.length) return null
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)

        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return value to index
    }
}
