package com.abrarshakhi.dourdiary.common.domain.model

@JvmInline
value class Speed(val metersPerSecond: Double) : Comparable<Speed> {

    init {
        require(metersPerSecond >= 0.0 && !metersPerSecond.isNaN()) {
            "Speed must be non-negative and finite, was $metersPerSecond"
        }
    }

    val kilometersPerHour: Double get() = metersPerSecond * 3.6

    override fun compareTo(other: Speed): Int = metersPerSecond.compareTo(other.metersPerSecond)

    companion object {
        val Zero = Speed(0.0)

        fun of(distance: Distance, durationMillis: Long): Speed? {
            if (durationMillis <= 0L) return null
            return Speed(distance.meters / (durationMillis / 1_000.0))
        }
    }
}
