package com.abrarshakhi.dourdiary.common.domain.model

data class TrackPoint(
    val point: GeoPoint,
    val elapsedRealtimeMillis: Long,
    val epochMillis: Long,
    val cumulativeDistance: Distance,
)
