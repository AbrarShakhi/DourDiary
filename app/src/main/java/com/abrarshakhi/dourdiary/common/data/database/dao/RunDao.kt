package com.abrarshakhi.dourdiary.common.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abrarshakhi.dourdiary.common.data.database.entity.RunEntity
import kotlinx.coroutines.flow.Flow

data class RunTotalsRow(
    val runCount: Int,
    val totalDistanceMeters: Double,
    val totalMovingDurationMillis: Long,
)

@Dao
interface RunDao {

    @Insert
    suspend fun insert(run: RunEntity): Long

    @Query("SELECT * FROM runs WHERE isComplete = 1 ORDER BY startedAtEpochMillis DESC")
    fun observeCompleted(): Flow<List<RunEntity>>

    @Query(
        "SELECT * FROM runs WHERE isComplete = 1 ORDER BY startedAtEpochMillis DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<RunEntity>>

    @Query(
        """
        SELECT * FROM runs
        WHERE isComplete = 1 AND startedAtEpochMillis >= :startEpochMillis
        ORDER BY startedAtEpochMillis DESC
        """,
    )
    fun observeSince(startEpochMillis: Long): Flow<List<RunEntity>>

    @Query("UPDATE runs SET simplifiedRoute = :polyline WHERE id = :runId")
    suspend fun updateSimplifiedRoute(runId: Long, polyline: String)

    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getById(runId: Long): RunEntity?

    @Query(
        "SELECT * FROM runs WHERE isComplete = 0 ORDER BY startedAtEpochMillis DESC LIMIT 1",
    )
    suspend fun findInProgress(): RunEntity?

    @Query(
        """
        SELECT COUNT(*) AS runCount,
               COALESCE(SUM(distanceMeters), 0) AS totalDistanceMeters,
               COALESCE(SUM(movingDurationMillis), 0) AS totalMovingDurationMillis
        FROM runs
        WHERE isComplete = 1
        """,
    )
    fun observeTotals(): Flow<RunTotalsRow>

    @Query(
        """
        UPDATE runs
        SET distanceMeters = :distanceMeters,
            movingDurationMillis = :movingDurationMillis,
            elapsedDurationMillis = :elapsedDurationMillis,
            accumulatedPausedMillis = :accumulatedPausedMillis
        WHERE id = :runId
        """,
    )
    suspend fun updateProgress(
        runId: Long,
        distanceMeters: Double,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    )

    @Query(
        """
        UPDATE runs
        SET isComplete = 1,
            finishedAtEpochMillis = :finishedAtEpochMillis,
            distanceMeters = :distanceMeters,
            movingDurationMillis = :movingDurationMillis,
            elapsedDurationMillis = :elapsedDurationMillis,
            accumulatedPausedMillis = :accumulatedPausedMillis
        WHERE id = :runId
        """,
    )
    suspend fun complete(
        runId: Long,
        finishedAtEpochMillis: Long,
        distanceMeters: Double,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    )

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun delete(runId: Long)
}
