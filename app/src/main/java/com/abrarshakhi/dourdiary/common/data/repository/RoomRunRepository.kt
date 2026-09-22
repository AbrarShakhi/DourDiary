package com.abrarshakhi.dourdiary.common.data.repository

import com.abrarshakhi.dourdiary.common.data.database.dao.RunDao
import com.abrarshakhi.dourdiary.common.data.database.dao.RunPointDao
import com.abrarshakhi.dourdiary.common.data.database.entity.RunEntity
import com.abrarshakhi.dourdiary.common.data.database.toDomain
import com.abrarshakhi.dourdiary.common.data.database.toEntity
import com.abrarshakhi.dourdiary.common.domain.geo.PolylineCodec
import com.abrarshakhi.dourdiary.common.domain.geo.RouteSimplifier
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.RunTotals
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomRunRepository(
    private val runDao: RunDao,
    private val runPointDao: RunPointDao,
    private val ioDispatcher: CoroutineDispatcher,
) : RunRepository {

    override fun observeCompletedRuns(): Flow<List<Run>> =
        runDao.observeCompleted()
            .map { runs -> runs.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeRecentRuns(limit: Int): Flow<List<Run>> =
        runDao.observeRecent(limit)
            .map { runs -> runs.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeRunsSince(startEpochMillis: Long): Flow<List<Run>> =
        runDao.observeSince(startEpochMillis)
            .map { runs -> runs.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeTotals(): Flow<RunTotals> =
        runDao.observeTotals()
            .map { it.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeRoute(runId: Long): Flow<List<TrackPoint>> =
        runPointDao.observeForRun(runId)
            .map { points -> points.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getRun(runId: Long): Run? = withContext(ioDispatcher) {
        runDao.getById(runId)?.toDomain()
    }

    override suspend fun getRoute(runId: Long): List<TrackPoint> = withContext(ioDispatcher) {
        runPointDao.getForRun(runId).map { it.toDomain() }
    }

    override suspend fun findInProgressRun(): Run? = withContext(ioDispatcher) {
        runDao.findInProgress()?.toDomain()
    }

    override suspend fun startRun(startedAtEpochMillis: Long): Long = withContext(ioDispatcher) {
        runDao.insert(RunEntity(startedAtEpochMillis = startedAtEpochMillis))
    }

    override suspend fun appendPoints(runId: Long, points: List<TrackPoint>) {
        if (points.isEmpty()) return
        withContext(ioDispatcher) {
            runPointDao.insertAll(points.map { it.toEntity(runId) })
        }
    }

    override suspend fun updateProgress(
        runId: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    ) = withContext(ioDispatcher) {
        runDao.updateProgress(
            runId = runId,
            distanceMeters = distance.meters,
            movingDurationMillis = movingDurationMillis,
            elapsedDurationMillis = elapsedDurationMillis,
            accumulatedPausedMillis = accumulatedPausedMillis,
        )
    }

    override suspend fun completeRun(
        runId: Long,
        finishedAtEpochMillis: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    ) = withContext(ioDispatcher) {
        runDao.complete(
            runId = runId,
            finishedAtEpochMillis = finishedAtEpochMillis,
            distanceMeters = distance.meters,
            movingDurationMillis = movingDurationMillis,
            elapsedDurationMillis = elapsedDurationMillis,
            accumulatedPausedMillis = accumulatedPausedMillis,
        )
        storeRouteSummary(runId)
    }

    private suspend fun storeRouteSummary(runId: Long) {
        runCatching {
            val route = runPointDao.getForRun(runId).map { it.toDomain().point }
            if (route.size < 2) return
            val simplified = RouteSimplifier.simplify(route)
            runDao.updateSimplifiedRoute(runId, PolylineCodec.encode(simplified))
        }
    }

    override suspend fun deleteRun(runId: Long) = withContext(ioDispatcher) {
        runDao.delete(runId)
    }
}
