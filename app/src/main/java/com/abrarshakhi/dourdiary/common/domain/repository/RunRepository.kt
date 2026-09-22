package com.abrarshakhi.dourdiary.common.domain.repository

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.RunTotals
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

interface RunRepository {

    fun observeCompletedRuns(): Flow<List<Run>>

    fun observeRecentRuns(limit: Int): Flow<List<Run>>

    fun observeRunsSince(startEpochMillis: Long): Flow<List<Run>>

    fun observeTotals(): Flow<RunTotals>

    fun observeRoute(runId: Long): Flow<List<TrackPoint>>

    suspend fun getRun(runId: Long): Run?

    suspend fun getRoute(runId: Long): List<TrackPoint>

    suspend fun findInProgressRun(): Run?

    suspend fun startRun(startedAtEpochMillis: Long): Long

    suspend fun appendPoints(runId: Long, points: List<TrackPoint>)

    suspend fun updateProgress(
        runId: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    )

    suspend fun completeRun(
        runId: Long,
        finishedAtEpochMillis: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    )

    suspend fun deleteRun(runId: Long)
}
