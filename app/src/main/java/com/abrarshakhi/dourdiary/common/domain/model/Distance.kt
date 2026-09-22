package com.abrarshakhi.dourdiary.common.domain.model

import kotlin.math.abs

private const val MetersPerKilometer = 1_000.0
private const val MetersPerMile = 1_609.344

@JvmInline
value class Distance(val meters: Double) : Comparable<Distance> {

    init {
        require(meters >= 0.0 && !meters.isNaN()) { "Distance must be non-negative and finite, was $meters" }
    }

    val inKilometers: Double get() = meters / MetersPerKilometer
    val inMiles: Double get() = meters / MetersPerMile

    operator fun plus(other: Distance): Distance = Distance(meters + other.meters)

    operator fun minus(other: Distance): Distance =
        Distance((meters - other.meters).coerceAtLeast(0.0))

    operator fun times(factor: Double): Distance = Distance(meters * factor)

    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)

    fun inUnit(unitSystem: UnitSystem): Double = when (unitSystem) {
        UnitSystem.METRIC -> inKilometers
        UnitSystem.IMPERIAL -> inMiles
    }

    companion object {
        val Zero = Distance(0.0)

        fun ofKilometers(kilometers: Double): Distance = Distance(kilometers * MetersPerKilometer)

        fun ofMiles(miles: Double): Distance = Distance(miles * MetersPerMile)
    }
}

fun Distance.isCloseTo(other: Distance, toleranceMeters: Double): Boolean =
    abs(meters - other.meters) <= toleranceMeters
