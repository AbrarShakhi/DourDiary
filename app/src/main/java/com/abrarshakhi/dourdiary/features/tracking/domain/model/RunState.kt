package com.abrarshakhi.dourdiary.features.tracking.domain.model

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.AutoPauseState
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.PaceSample
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.RejectionReason

data class RunState(
    val status: RunStatus = RunStatus.IDLE,
    val startedAtElapsedRealtimeMillis: Long = 0L,
    val startedAtEpochMillis: Long = 0L,
    val finishedAtElapsedRealtimeMillis: Long? = null,
    val accumulatedPausedMillis: Long = 0L,
    val pausedAtElapsedRealtimeMillis: Long? = null,
    val distance: Distance = Distance.Zero,
    val lastPoint: TrackPoint? = null,
    val autoPause: AutoPauseState = AutoPauseState(),
    val paceSamples: List<PaceSample> = emptyList(),
    val lastRejection: RejectionReason? = null,
    val reanchorPending: Boolean = false,
) {
    private fun clockAt(nowElapsedRealtimeMillis: Long): Long =
        finishedAtElapsedRealtimeMillis ?: nowElapsedRealtimeMillis

    fun elapsedDurationMillis(nowElapsedRealtimeMillis: Long): Long {
        if (status == RunStatus.IDLE) return 0L
        return (clockAt(nowElapsedRealtimeMillis) - startedAtElapsedRealtimeMillis)
            .coerceAtLeast(0L)
    }

    fun movingDurationMillis(nowElapsedRealtimeMillis: Long): Long {
        if (status == RunStatus.IDLE) return 0L
        val now = clockAt(nowElapsedRealtimeMillis)
        val currentPause = pausedAtElapsedRealtimeMillis?.let { (now - it).coerceAtLeast(0L) } ?: 0L
        return (now - startedAtElapsedRealtimeMillis - accumulatedPausedMillis - currentPause)
            .coerceAtLeast(0L)
    }
}
