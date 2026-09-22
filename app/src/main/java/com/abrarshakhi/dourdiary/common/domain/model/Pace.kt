package com.abrarshakhi.dourdiary.common.domain.model

@JvmInline
value class Pace(val secondsPerKilometer: Double) : Comparable<Pace> {

    init {
        require(secondsPerKilometer > 0.0 && secondsPerKilometer.isFinite()) {
            "Pace must be positive and finite, was $secondsPerKilometer"
        }
    }

    val secondsPerMile: Double get() = secondsPerKilometer * 1.609344

    fun secondsPerUnit(unitSystem: UnitSystem): Double = when (unitSystem) {
        UnitSystem.METRIC -> secondsPerKilometer
        UnitSystem.IMPERIAL -> secondsPerMile
    }

    override fun compareTo(other: Pace): Int =
        secondsPerKilometer.compareTo(other.secondsPerKilometer)

    companion object {
        val MinimumMeaningfulDistance = Distance(50.0)

        fun ofRun(distance: Distance, durationMillis: Long): Pace? {
            if (distance < MinimumMeaningfulDistance) return null
            return of(distance, durationMillis)
        }

        fun fromSpeed(speed: Speed): Pace? {
            if (speed.metersPerSecond <= 0.0) return null
            return Pace(1_000.0 / speed.metersPerSecond)
        }

        fun of(distance: Distance, durationMillis: Long): Pace? {
            if (distance.meters <= 0.0 || durationMillis <= 0L) return null
            return Pace((durationMillis / 1_000.0) / distance.inKilometers)
        }
    }
}
