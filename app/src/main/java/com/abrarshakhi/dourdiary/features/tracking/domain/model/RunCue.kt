package com.abrarshakhi.dourdiary.features.tracking.domain.model

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Pace

data class RunCue(
    val milestone: Int,
    val distance: Distance,
    val movingDurationMillis: Long,
    val averagePace: Pace?,
    val lastSplitPace: Pace?,
)
