package com.abrarshakhi.dourdiary.features.tracking.data.session

import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Pace
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.features.tracking.domain.TrackingClock
import com.abrarshakhi.dourdiary.features.tracking.domain.datasource.LocationDataSource
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.CueScheduler
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.PaceCalculator
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.RunEngine
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunCue
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunState
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RunSessionManager(
    private val locationDataSource: LocationDataSource,
    private val runRepository: RunRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val clock: TrackingClock,
    private val engine: RunEngine,
    private val scope: CoroutineScope,
    private val config: TrackingConfig = TrackingConfig(),
) {
    private val mutex = Mutex()

    private var state = RunState()

    private val _snapshot = MutableStateFlow(RunSnapshot())
    val snapshot: StateFlow<RunSnapshot> = _snapshot.asStateFlow()

    private val _activeRunId = MutableStateFlow<Long?>(null)
    val activeRunId: StateFlow<Long?> = _activeRunId.asStateFlow()

    private val _recoverableRun = MutableStateFlow<Run?>(null)
    val recoverableRun: StateFlow<Run?> = _recoverableRun.asStateFlow()

    private val _route = MutableStateFlow<List<GeoPoint>>(emptyList())
    val route: StateFlow<List<GeoPoint>> = _route.asStateFlow()

    private val _cues = MutableSharedFlow<RunCue>(replay = 0, extraBufferCapacity = 8)
    val cues: SharedFlow<RunCue> = _cues.asSharedFlow()

    private val _finishedRuns = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 4)
    val finishedRuns: SharedFlow<Long> = _finishedRuns.asSharedFlow()

    private var locationJob: Job? = null
    private var tickJob: Job? = null

    @Volatile
    private var preferences = AppPreferences()

    private val pendingPoints = mutableListOf<TrackPoint>()
    private var lastCheckpointElapsedMillis = 0L
    private var lastCuedDistance = Distance.Zero
    private var lastMilestoneElapsedMillis = 0L

    init {
        preferencesRepository.preferences
            .onEach { preferences = it }
            .launchIn(scope)
    }

    fun start() {
        scope.launch {
            mutex.withLock {
                if (state.status.isRecording) return@withLock
                val startedAtEpoch = clock.epochMillis()
                val runId = runRepository.startRun(startedAtEpoch)
                val now = clock.elapsedRealtimeMillis()
                state = engine.start(now, startedAtEpoch)
                _activeRunId.value = runId
                _recoverableRun.value = null
                _route.value = emptyList()
                lastCheckpointElapsedMillis = now
                lastCuedDistance = Distance.Zero
                lastMilestoneElapsedMillis = now
                publish()
            }
            observeLocation()
            startTicking()
        }
    }

    fun pause() = mutate { engine.pause(it, clock.elapsedRealtimeMillis()) }

    fun resume() = mutate { engine.resume(it, clock.elapsedRealtimeMillis()) }

    fun stop() {
        scope.launch {
            locationJob?.cancel()
            tickJob?.cancel()
            mutex.withLock {
                val runId = _activeRunId.value ?: return@withLock
                val now = clock.elapsedRealtimeMillis()
                state = engine.stop(state, now)
                flushPendingPoints(runId)
                runRepository.completeRun(
                    runId = runId,
                    finishedAtEpochMillis = clock.epochMillis(),
                    distance = state.distance,
                    movingDurationMillis = state.movingDurationMillis(now),
                    elapsedDurationMillis = state.elapsedDurationMillis(now),
                    accumulatedPausedMillis = state.accumulatedPausedMillis,
                )
                publish()
                state = RunState()
                _activeRunId.value = null
                _route.value = emptyList()
                publish()
                _finishedRuns.tryEmit(runId)
            }
        }
    }

    suspend fun checkForRecoverableRun() {
        mutex.withLock {
            if (state.status.isRecording) return@withLock
            _recoverableRun.value = runRepository.findInProgressRun()
        }
    }

    fun resumeRecoveredRun() {
        scope.launch {
            mutex.withLock {
                val run = _recoverableRun.value ?: return@withLock
                val now = clock.elapsedRealtimeMillis()
                state = RunState(
                    status = RunStatus.PAUSED,
                    startedAtElapsedRealtimeMillis = now - run.elapsedDurationMillis,
                    startedAtEpochMillis = run.startedAtEpochMillis,
                    accumulatedPausedMillis = run.accumulatedPausedMillis,
                    pausedAtElapsedRealtimeMillis = now,
                    distance = run.distance,
                    lastPoint = null,
                    reanchorPending = true,
                )
                _activeRunId.value = run.id
                _recoverableRun.value = null
                _route.value = runRepository.getRoute(run.id).map { it.point }
                lastCheckpointElapsedMillis = now
                lastCuedDistance = run.distance
                lastMilestoneElapsedMillis = now
                publish()
            }
            observeLocation()
            startTicking()
        }
    }

    suspend fun discardRecoveredRun() {
        mutex.withLock {
            val run = _recoverableRun.value ?: return@withLock
            runRepository.completeRun(
                runId = run.id,
                finishedAtEpochMillis = clock.epochMillis(),
                distance = run.distance,
                movingDurationMillis = run.movingDurationMillis,
                elapsedDurationMillis = run.elapsedDurationMillis,
                accumulatedPausedMillis = run.accumulatedPausedMillis,
            )
            _recoverableRun.value = null
        }
    }

    private fun mutate(transform: (RunState) -> RunState) {
        scope.launch {
            mutex.withLock {
                state = transform(state)
                publish()
            }
        }
    }

    private fun observeLocation() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationDataSource.locationUpdates(LocationIntervalMillis)
                .catch { }
                .collect { sample ->
                    mutex.withLock {
                        val update = engine.onLocation(state, sample)
                        state = update.state

                        update.recordedPoint?.let { point ->
                            pendingPoints += point
                            _route.value = _route.value + point.point
                        }
                        emitCuesFor(sample.elapsedRealtimeMillis)
                        persistIfDue()
                        publish()
                    }
                }
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                mutex.withLock { publish() }
                delay(TickIntervalMillis)
            }
        }
    }

    private fun emitCuesFor(atElapsedMillis: Long) {
        val interval = Distance(preferences.unitSystem.cueUnit.meters * preferences.cueIntervalUnits)
        val crossed = CueScheduler.milestonesCrossed(lastCuedDistance, state.distance, interval)
        lastCuedDistance = state.distance
        if (crossed.isEmpty()) return

        if (!preferences.audioCuesEnabled) {
            lastMilestoneElapsedMillis = atElapsedMillis
            return
        }

        val movingMillis = state.movingDurationMillis(atElapsedMillis)
        crossed.forEach { milestone ->
            val milestoneDistance = Distance(interval.meters * milestone)
            _cues.tryEmit(
                RunCue(
                    milestone = milestone,
                    distance = milestoneDistance,
                    movingDurationMillis = movingMillis,
                    averagePace = PaceCalculator.averagePace(milestoneDistance, movingMillis),
                    lastSplitPace = splitPace(interval, atElapsedMillis),
                ),
            )
            lastMilestoneElapsedMillis = atElapsedMillis
        }
    }

    private fun splitPace(interval: Distance, atElapsedMillis: Long): Pace? =
        Pace.of(interval, atElapsedMillis - lastMilestoneElapsedMillis)

    private suspend fun persistIfDue() {
        val runId = _activeRunId.value ?: return
        val now = clock.elapsedRealtimeMillis()
        val checkpointDue = now - lastCheckpointElapsedMillis >= CheckpointIntervalMillis

        if (pendingPoints.size >= PointBatchSize || checkpointDue) {
            flushPendingPoints(runId)
        }
        if (checkpointDue) {
            lastCheckpointElapsedMillis = now
            runRepository.updateProgress(
                runId = runId,
                distance = state.distance,
                movingDurationMillis = state.movingDurationMillis(now),
                elapsedDurationMillis = state.elapsedDurationMillis(now),
                accumulatedPausedMillis = state.accumulatedPausedMillis,
            )
        }
    }

    private suspend fun flushPendingPoints(runId: Long) {
        if (pendingPoints.isEmpty()) return
        val batch = pendingPoints.toList()
        pendingPoints.clear()
        runRepository.appendPoints(runId, batch)
    }

    private fun publish() {
        _snapshot.value = engine.snapshot(state, clock.elapsedRealtimeMillis())
    }

    private companion object {
        const val LocationIntervalMillis = 1_000L
        const val TickIntervalMillis = 1_000L
        const val CheckpointIntervalMillis = 5_000L

        const val PointBatchSize = 10
    }
}
