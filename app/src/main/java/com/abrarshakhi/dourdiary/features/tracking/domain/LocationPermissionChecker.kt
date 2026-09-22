package com.abrarshakhi.dourdiary.features.tracking.domain

interface LocationPermissionChecker {
    fun hasLocationPermission(): Boolean
}
