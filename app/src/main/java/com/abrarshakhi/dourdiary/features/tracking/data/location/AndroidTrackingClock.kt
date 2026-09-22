package com.abrarshakhi.dourdiary.features.tracking.data.location

import android.os.SystemClock
import com.abrarshakhi.dourdiary.features.tracking.domain.TrackingClock

class AndroidTrackingClock : TrackingClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun epochMillis(): Long = System.currentTimeMillis()
}
