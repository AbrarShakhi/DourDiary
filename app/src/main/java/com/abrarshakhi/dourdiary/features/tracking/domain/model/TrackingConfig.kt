package com.abrarshakhi.dourdiary.features.tracking.domain.model

data class TrackingConfig(
    val maxAccuracyMeters: Double = 25.0,

    val minDisplacementMeters: Double = 3.0,

    val maxPlausibleSpeedMetersPerSecond: Double = 12.0,

    val autoPauseSpeedThresholdMetersPerSecond: Double = 0.5,

    val autoPauseEnterAfterMillis: Long = 8_000L,

    val autoPauseExitAfterMillis: Long = 3_000L,

    val paceWindowMillis: Long = 30_000L,

    val minimumRollingPaceDistanceMeters: Double = 20.0,

    val autoPauseEnabled: Boolean = true,
) {
    init {
        require(maxAccuracyMeters > 0) { "maxAccuracyMeters must be positive" }
        require(minDisplacementMeters >= 0) { "minDisplacementMeters must be non-negative" }
        require(maxPlausibleSpeedMetersPerSecond > 0) { "maxPlausibleSpeed must be positive" }
        require(paceWindowMillis > 0) { "paceWindowMillis must be positive" }
    }
}
