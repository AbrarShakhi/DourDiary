package com.abrarshakhi.dourdiary.features.tracking.domain.model

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint

data class LocationSample(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val elapsedRealtimeMillis: Long,
    val epochMillis: Long,
    val speedMetersPerSecond: Double? = null,
)
