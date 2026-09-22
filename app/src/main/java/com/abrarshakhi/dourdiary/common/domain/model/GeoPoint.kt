package com.abrarshakhi.dourdiary.common.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
    }
}
