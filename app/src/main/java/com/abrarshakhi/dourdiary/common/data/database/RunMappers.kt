package com.abrarshakhi.dourdiary.common.data.database

import com.abrarshakhi.dourdiary.common.data.database.dao.RunTotalsRow
import com.abrarshakhi.dourdiary.common.data.database.entity.RunEntity
import com.abrarshakhi.dourdiary.common.data.database.entity.RunPointEntity
import com.abrarshakhi.dourdiary.common.domain.geo.PolylineCodec
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.RunTotals
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint

internal fun RunEntity.toDomain(): Run = Run(
    id = id,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    distance = Distance(distanceMeters.coerceAtLeast(0.0)),
    movingDurationMillis = movingDurationMillis,
    elapsedDurationMillis = elapsedDurationMillis,
    accumulatedPausedMillis = accumulatedPausedMillis,
    isComplete = isComplete,
    simplifiedRoute = simplifiedRoute?.let(PolylineCodec::decode).orEmpty(),
)

internal fun RunPointEntity.toDomain(): TrackPoint = TrackPoint(
    point = GeoPoint(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitudeMeters,
    ),
    elapsedRealtimeMillis = elapsedRealtimeMillis,
    epochMillis = epochMillis,
    cumulativeDistance = Distance(cumulativeDistanceMeters.coerceAtLeast(0.0)),
)

internal fun TrackPoint.toEntity(runId: Long): RunPointEntity = RunPointEntity(
    runId = runId,
    latitude = point.latitude,
    longitude = point.longitude,
    altitudeMeters = point.altitudeMeters,
    epochMillis = epochMillis,
    elapsedRealtimeMillis = elapsedRealtimeMillis,
    cumulativeDistanceMeters = cumulativeDistance.meters,
)

internal fun RunTotalsRow.toDomain(): RunTotals = RunTotals(
    runCount = runCount,
    totalDistance = Distance(totalDistanceMeters.coerceAtLeast(0.0)),
    totalMovingDurationMillis = totalMovingDurationMillis,
)
