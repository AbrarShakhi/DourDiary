package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample

class RunTrackBuilder(
    private val accuracyMeters: Double = 5.0,
    private val startEpochMillis: Long = 1_700_000_000_000L,
) {
    private val samples = mutableListOf<LocationSample>()
    private var offsetMeters = 0.0
    private var millis = 0L

    init {
        emit(speed = null)
    }

    fun move(seconds: Int, metersPerSecond: Double) = apply {
        repeat(seconds) {
            millis += 1_000L
            offsetMeters += metersPerSecond
            emit(speed = metersPerSecond)
        }
    }

    fun standStill(seconds: Int) = apply {
        repeat(seconds) {
            millis += 1_000L
            emit(speed = 0.0)
        }
    }

    fun drift(seconds: Int, amplitudeMeters: Double = 1.5) = apply {
        val anchor = offsetMeters
        repeat(seconds) { second ->
            millis += 1_000L
            offsetMeters = anchor + if (second % 2 == 0) amplitudeMeters else -amplitudeMeters
            emit(speed = 0.0)
        }
        offsetMeters = anchor
    }

    fun jump(meters: Double) = apply {
        millis += 1_000L
        offsetMeters += meters
        emit(speed = null)
    }

    fun loseSignal(seconds: Int, metersPerSecond: Double = 3.0) = apply {
        repeat(seconds) {
            millis += 1_000L
            offsetMeters += metersPerSecond
            emit(speed = metersPerSecond, accuracy = 80.0)
        }
    }

    fun silence(seconds: Int) = apply { millis += seconds * 1_000L }

    val elapsedMillis: Long get() = millis

    fun build(): List<LocationSample> = samples.toList()

    private fun emit(speed: Double?, accuracy: Double = accuracyMeters) {
        samples += LocationSample(
            point = GeoPoint(
                latitude = OriginLatitude + offsetMeters / MetersPerDegreeLatitude,
                longitude = OriginLongitude,
            ),
            accuracyMeters = accuracy,
            elapsedRealtimeMillis = millis,
            epochMillis = startEpochMillis + millis,
            speedMetersPerSecond = speed,
        )
    }

    companion object {
        const val MetersPerDegreeLatitude = 111_195.08
        const val OriginLatitude = 52.5200
        const val OriginLongitude = 13.4050
    }
}
