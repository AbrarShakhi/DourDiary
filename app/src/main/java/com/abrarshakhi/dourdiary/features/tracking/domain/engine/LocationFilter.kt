package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.geo.GeoMath
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig

enum class RejectionReason {
    POOR_ACCURACY,

    OUT_OF_ORDER,

    BELOW_MIN_DISPLACEMENT,

    IMPLAUSIBLE_SPEED,
}

sealed interface FilterVerdict {
    data class Accept(val legDistance: Distance) : FilterVerdict

    data class Reject(val reason: RejectionReason) : FilterVerdict
}

class LocationFilter(private val config: TrackingConfig) {

    fun evaluate(previous: TrackPoint?, candidate: LocationSample): FilterVerdict {
        if (candidate.accuracyMeters > config.maxAccuracyMeters) {
            return FilterVerdict.Reject(RejectionReason.POOR_ACCURACY)
        }

        if (previous == null) return FilterVerdict.Accept(Distance.Zero)

        val elapsedMillis = candidate.elapsedRealtimeMillis - previous.elapsedRealtimeMillis
        if (elapsedMillis <= 0L) {
            return FilterVerdict.Reject(RejectionReason.OUT_OF_ORDER)
        }

        val legDistance = GeoMath.distanceBetween(previous.point, candidate.point)

        val impliedSpeed = legDistance.meters / (elapsedMillis / 1_000.0)
        if (impliedSpeed > config.maxPlausibleSpeedMetersPerSecond) {
            return FilterVerdict.Reject(RejectionReason.IMPLAUSIBLE_SPEED)
        }

        if (legDistance.meters < config.minDisplacementMeters) {
            return FilterVerdict.Reject(RejectionReason.BELOW_MIN_DISPLACEMENT)
        }

        return FilterVerdict.Accept(legDistance)
    }
}
