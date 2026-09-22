package com.abrarshakhi.dourdiary.common.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    indices = [Index("isComplete"), Index("startedAtEpochMillis")],
)
data class RunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val startedAtEpochMillis: Long,

    val finishedAtEpochMillis: Long? = null,

    @ColumnInfo(defaultValue = "0")
    val distanceMeters: Double = 0.0,

    @ColumnInfo(defaultValue = "0")
    val movingDurationMillis: Long = 0L,

    @ColumnInfo(defaultValue = "0")
    val elapsedDurationMillis: Long = 0L,

    @ColumnInfo(defaultValue = "0")
    val accumulatedPausedMillis: Long = 0L,

    @ColumnInfo(defaultValue = "0")
    val isComplete: Boolean = false,

    val simplifiedRoute: String? = null,
)
