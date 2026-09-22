package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.geo.GeoMath
import com.abrarshakhi.dourdiary.common.domain.model.Speed
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunState
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig

data class RunUpdate(
    val state: RunState,
    val recordedPoint: TrackPoint? = null,
)

class RunEngine(
    private val config: TrackingConfig = TrackingConfig(),
    private val filter: LocationFilter = LocationFilter(config),
    private val autoPauseDetector: AutoPauseDetector = AutoPauseDetector(config),
) {

    fun start(nowElapsedRealtimeMillis: Long, nowEpochMillis: Long): RunState = RunState(
        status = RunStatus.ACTIVE,
        startedAtElapsedRealtimeMillis = nowElapsedRealtimeMillis,
        startedAtEpochMillis = nowEpochMillis,
    )

    fun pause(state: RunState, nowElapsedRealtimeMillis: Long): RunState {
        if (state.status != RunStatus.ACTIVE && state.status != RunStatus.AUTO_PAUSED) return state
        return state.copy(
            status = RunStatus.PAUSED,
            pausedAtElapsedRealtimeMillis = state.pausedAtElapsedRealtimeMillis
                ?: nowElapsedRealtimeMillis,
            autoPause = AutoPauseState(),
        )
    }

    fun resume(state: RunState, nowElapsedRealtimeMillis: Long): RunState {
        if (!state.status.isPaused) return state
        val pausedFor = state.pausedAtElapsedRealtimeMillis
            ?.let { (nowElapsedRealtimeMillis - it).coerceAtLeast(0L) }
            ?: 0L
        return state.copy(
            status = RunStatus.ACTIVE,
            accumulatedPausedMillis = state.accumulatedPausedMillis + pausedFor,
            pausedAtElapsedRealtimeMillis = null,
            autoPause = AutoPauseState(),
            reanchorPending = true,
        )
    }

    fun stop(state: RunState, nowElapsedRealtimeMillis: Long): RunState {
        if (state.status == RunStatus.IDLE || state.status == RunStatus.FINISHED) return state
        val pausedFor = state.pausedAtElapsedRealtimeMillis
            ?.let { (nowElapsedRealtimeMillis - it).coerceAtLeast(0L) }
            ?: 0L
        return state.copy(
            status = RunStatus.FINISHED,
            finishedAtElapsedRealtimeMillis = nowElapsedRealtimeMillis,
            accumulatedPausedMillis = state.accumulatedPausedMillis + pausedFor,
            pausedAtElapsedRealtimeMillis = null,
        )
    }

    fun onLocation(state: RunState, sample: LocationSample): RunUpdate {
        if (state.status != RunStatus.ACTIVE && state.status != RunStatus.AUTO_PAUSED) {
            return RunUpdate(state)
        }

        val verdict = filter.evaluate(state.lastPoint, sample)

        if (verdict is FilterVerdict.Reject &&
            verdict.reason != RejectionReason.BELOW_MIN_DISPLACEMENT
        ) {
            return RunUpdate(state.copy(lastRejection = verdict.reason))
        }

        val paused = applyAutoPause(state, sample)

        if (verdict !is FilterVerdict.Accept) {
            return RunUpdate(paused.copy(lastRejection = RejectionReason.BELOW_MIN_DISPLACEMENT))
        }

        return recordPoint(paused, sample, verdict)
    }

    fun snapshot(state: RunState, nowElapsedRealtimeMillis: Long): RunSnapshot {
        val movingMillis = state.movingDurationMillis(nowElapsedRealtimeMillis)
        return RunSnapshot(
            status = state.status,
            distance = state.distance,
            movingDurationMillis = movingMillis,
            elapsedDurationMillis = state.elapsedDurationMillis(nowElapsedRealtimeMillis),
            currentPace = if (state.status == RunStatus.ACTIVE) {
PaceCalculator.rollingPace(
                    samples = state.paceSamples,
                    windowMillis = config.paceWindowMillis,
                    minimumDistanceMeters = config.minimumRollingPaceDistanceMeters,
                )
            } else {
                null
            },
            averagePace = PaceCalculator.averagePace(state.distance, movingMillis),
            lastPoint = state.lastPoint?.point,
            hasPoorSignal = state.lastRejection == RejectionReason.POOR_ACCURACY,
        )
    }

    private fun applyAutoPause(state: RunState, sample: LocationSample): RunState {
        val speed = impliedSpeed(state.lastPoint, sample) ?: return state
        val autoPause = autoPauseDetector.next(state.autoPause, speed, sample.elapsedRealtimeMillis)

        return when {
            state.status == RunStatus.ACTIVE && autoPause.paused -> state.copy(
                status = RunStatus.AUTO_PAUSED,
                pausedAtElapsedRealtimeMillis = sample.elapsedRealtimeMillis,
                autoPause = autoPause,
            )

            state.status == RunStatus.AUTO_PAUSED && !autoPause.paused -> {
                val pausedFor = state.pausedAtElapsedRealtimeMillis
                    ?.let { (sample.elapsedRealtimeMillis - it).coerceAtLeast(0L) }
                    ?: 0L
                state.copy(
                    status = RunStatus.ACTIVE,
                    accumulatedPausedMillis = state.accumulatedPausedMillis + pausedFor,
                    pausedAtElapsedRealtimeMillis = null,
                    autoPause = autoPause,
                    reanchorPending = true,
                )
            }

            else -> state.copy(autoPause = autoPause)
        }
    }

    private fun recordPoint(
        state: RunState,
        sample: LocationSample,
        verdict: FilterVerdict.Accept,
    ): RunUpdate {
        val countsTowardsDistance =
            state.status == RunStatus.ACTIVE && !state.reanchorPending && state.lastPoint != null

        val distance = if (countsTowardsDistance) state.distance + verdict.legDistance else state.distance

        val point = TrackPoint(
            point = sample.point,
            elapsedRealtimeMillis = sample.elapsedRealtimeMillis,
            epochMillis = sample.epochMillis,
            cumulativeDistance = distance,
        )

        val paceSamples = PaceCalculator.prune(
            samples = state.paceSamples + PaceSample(
                elapsedRealtimeMillis = sample.elapsedRealtimeMillis,
                cumulativeDistanceMeters = distance.meters,
            ),
            windowMillis = config.paceWindowMillis,
        )

        return RunUpdate(
            state = state.copy(
                distance = distance,
                lastPoint = point,
                paceSamples = paceSamples,
                lastRejection = null,
                reanchorPending = false,
            ),
            recordedPoint = point,
        )
    }

    private fun impliedSpeed(previous: TrackPoint?, sample: LocationSample): Speed? {
        sample.speedMetersPerSecond
            ?.takeIf { it >= 0.0 && it.isFinite() }
            ?.let { return Speed(it) }

        if (previous == null) return null
        val elapsedMillis = sample.elapsedRealtimeMillis - previous.elapsedRealtimeMillis
        if (elapsedMillis <= 0L) return null

        return Speed.of(GeoMath.distanceBetween(previous.point, sample.point), elapsedMillis)
    }
}
