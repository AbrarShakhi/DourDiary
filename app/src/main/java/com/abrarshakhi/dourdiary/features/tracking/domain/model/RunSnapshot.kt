package com.abrarshakhi.dourdiary.features.tracking.domain.model

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Pace

data class RunSnapshot(
    val status: RunStatus = RunStatus.IDLE,
    val distance: Distance = Distance.Zero,
    val movingDurationMillis: Long = 0L,
    val elapsedDurationMillis: Long = 0L,
    val currentPace: Pace? = null,
    val averagePace: Pace? = null,
    val lastPoint: GeoPoint? = null,
    val hasPoorSignal: Boolean = false,
)
