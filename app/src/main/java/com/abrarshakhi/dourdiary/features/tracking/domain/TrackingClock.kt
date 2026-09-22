package com.abrarshakhi.dourdiary.features.tracking.domain

interface TrackingClock {
    fun elapsedRealtimeMillis(): Long

    fun epochMillis(): Long
}
