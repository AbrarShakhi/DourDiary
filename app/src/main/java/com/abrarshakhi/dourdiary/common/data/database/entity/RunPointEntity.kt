package com.abrarshakhi.dourdiary.common.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_points",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId")],
)
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val runId: Long,

    val latitude: Double,

    val longitude: Double,

    val altitudeMeters: Double? = null,

    val epochMillis: Long,

    val elapsedRealtimeMillis: Long,

    val cumulativeDistanceMeters: Double,
)
