package com.abrarshakhi.dourdiary.common.domain.model

import java.time.LocalDate

data class DailyDistance(
    val date: LocalDate,
    val distance: Distance = Distance.Zero,
    val runCount: Int = 0,
)

data class WeeklySummary(
    val days: List<DailyDistance> = emptyList(),
    val totalDistance: Distance = Distance.Zero,
    val totalMovingDurationMillis: Long = 0L,
    val runCount: Int = 0,
) {
    val averagePace: Pace? get() = Pace.ofRun(totalDistance, totalMovingDurationMillis)

    val busiestDayDistance: Distance?
        get() = days.maxByOrNull { it.distance.meters }
            ?.distance
            ?.takeIf { it.meters > 0.0 }
}
