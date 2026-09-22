package com.abrarshakhi.dourdiary.common.domain.model

data class RunTotals(
    val runCount: Int = 0,
    val totalDistance: Distance = Distance.Zero,
    val totalMovingDurationMillis: Long = 0L,
) {
    val averagePace: Pace? get() = Pace.ofRun(totalDistance, totalMovingDurationMillis)
}
