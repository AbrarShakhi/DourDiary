package com.abrarshakhi.dourdiary.common.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abrarshakhi.dourdiary.common.data.database.entity.RunPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunPointDao {

    @Insert
    suspend fun insertAll(points: List<RunPointEntity>)

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY elapsedRealtimeMillis ASC")
    fun observeForRun(runId: Long): Flow<List<RunPointEntity>>

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY elapsedRealtimeMillis ASC")
    suspend fun getForRun(runId: Long): List<RunPointEntity>

    @Query("SELECT COUNT(*) FROM run_points WHERE runId = :runId")
    suspend fun countForRun(runId: Long): Int
}
