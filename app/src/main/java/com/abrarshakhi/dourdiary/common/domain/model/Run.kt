package com.abrarshakhi.dourdiary.common.domain.model

data class Run(
    val id: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?,
    val distance: Distance,
    val movingDurationMillis: Long,
    val elapsedDurationMillis: Long,
    val accumulatedPausedMillis: Long,
    val isComplete: Boolean,
    val simplifiedRoute: List<GeoPoint> = emptyList(),
) {
    val averagePace: Pace? get() = Pace.ofRun(distance, movingDurationMillis)
}
