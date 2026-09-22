package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Pace

data class PaceSample(
    val elapsedRealtimeMillis: Long,
    val cumulativeDistanceMeters: Double,
)

object PaceCalculator {
    fun rollingPace(
        samples: List<PaceSample>,
        windowMillis: Long,
        minimumDistanceMeters: Double = 0.0,
    ): Pace? {
        if (samples.size < 2) return null

        val newest = samples.last()
        val cutoff = newest.elapsedRealtimeMillis - windowMillis
        val anchor = samples.lastOrNull { it.elapsedRealtimeMillis <= cutoff } ?: samples.first()

        val elapsedMillis = newest.elapsedRealtimeMillis - anchor.elapsedRealtimeMillis
        val distanceMeters = newest.cumulativeDistanceMeters - anchor.cumulativeDistanceMeters
        if (elapsedMillis <= 0L || distanceMeters <= 0.0) return null
        if (distanceMeters < minimumDistanceMeters) return null

        return Pace.of(Distance(distanceMeters), elapsedMillis)
    }

    fun averagePace(distance: Distance, movingDurationMillis: Long): Pace? =
        Pace.ofRun(distance, movingDurationMillis)

    fun prune(samples: List<PaceSample>, windowMillis: Long): List<PaceSample> {
        if (samples.size < 2) return samples
        val cutoff = samples.last().elapsedRealtimeMillis - windowMillis
        val firstNeeded = samples.indexOfLast { it.elapsedRealtimeMillis <= cutoff }
        return if (firstNeeded <= 0) samples else samples.subList(firstNeeded, samples.size)
    }
}
