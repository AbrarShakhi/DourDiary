package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import kotlin.math.floor

object CueScheduler {
    fun milestonesCrossed(
        previous: Distance,
        current: Distance,
        interval: Distance,
    ): List<Int> {
        if (interval.meters <= 0.0) return emptyList()
        if (current <= previous) return emptyList()

        val previousMilestone = floor(previous.meters / interval.meters).toInt()
        val currentMilestone = floor(current.meters / interval.meters).toInt()
        if (currentMilestone <= previousMilestone) return emptyList()

        return ((previousMilestone + 1)..currentMilestone).toList()
    }
}
