package com.abrarshakhi.dourdiary.features.tracking.data.session

import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.RunTotals
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.features.tracking.domain.TrackingClock
import com.abrarshakhi.dourdiary.features.tracking.domain.datasource.LocationDataSource
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf

class FakeTrackingClock(
    var elapsed: Long = 0L,
    var epoch: Long = 1_700_000_000_000L,
) : TrackingClock {
    override fun elapsedRealtimeMillis(): Long = elapsed
    override fun epochMillis(): Long = epoch
}

class FakeLocationDataSource : LocationDataSource {
    val fixes = MutableSharedFlow<LocationSample>(extraBufferCapacity = 256)
    var requestedIntervalMillis: Long? = null

    override fun locationUpdates(intervalMillis: Long): Flow<LocationSample> {
        requestedIntervalMillis = intervalMillis
        return fixes
    }
}

class FakeRunRepository : RunRepository {
    private val state = MutableStateFlow<Map<Long, Run>>(emptyMap())

    val runs: Map<Long, Run> get() = state.value
    val points = linkedMapOf<Long, MutableList<TrackPoint>>()

    fun put(run: Run) {
        state.value = state.value + (run.id to run)
    }

    var appendCallCount = 0
        private set
    var progressUpdateCount = 0
        private set

    private var nextId = 1L

    override fun observeCompletedRuns(): Flow<List<Run>> =
        state.map { runs -> runs.values.filter { it.isComplete } }

    override fun observeRecentRuns(limit: Int): Flow<List<Run>> =
        state.map { runs ->
            runs.values.filter { it.isComplete }
                .sortedByDescending { it.startedAtEpochMillis }
                .take(limit)
        }

    override fun observeRunsSince(startEpochMillis: Long): Flow<List<Run>> =
        state.map { runs ->
            runs.values.filter { it.isComplete && it.startedAtEpochMillis >= startEpochMillis }
        }

    override fun observeTotals(): Flow<RunTotals> = flowOf(RunTotals())

    override fun observeRoute(runId: Long): Flow<List<TrackPoint>> =
        flowOf(points[runId].orEmpty())

    override suspend fun getRun(runId: Long): Run? = runs[runId]

    override suspend fun getRoute(runId: Long): List<TrackPoint> = points[runId].orEmpty()

    override suspend fun findInProgressRun(): Run? = runs.values.lastOrNull { !it.isComplete }

    override suspend fun startRun(startedAtEpochMillis: Long): Long {
        val id = nextId++
        put(
            Run(
                id = id,
                startedAtEpochMillis = startedAtEpochMillis,
                finishedAtEpochMillis = null,
                distance = Distance.Zero,
                movingDurationMillis = 0L,
                elapsedDurationMillis = 0L,
                accumulatedPausedMillis = 0L,
                isComplete = false,
            ),
        )
        return id
    }

    override suspend fun appendPoints(runId: Long, points: List<TrackPoint>) {
        if (points.isEmpty()) return
        appendCallCount++
        this.points.getOrPut(runId) { mutableListOf() }.addAll(points)
    }

    override suspend fun updateProgress(
        runId: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    ) {
        progressUpdateCount++
        put(
            runs.getValue(runId).copy(
                distance = distance,
                movingDurationMillis = movingDurationMillis,
                elapsedDurationMillis = elapsedDurationMillis,
                accumulatedPausedMillis = accumulatedPausedMillis,
            ),
        )
    }

    override suspend fun completeRun(
        runId: Long,
        finishedAtEpochMillis: Long,
        distance: Distance,
        movingDurationMillis: Long,
        elapsedDurationMillis: Long,
        accumulatedPausedMillis: Long,
    ) {
        put(
            runs.getValue(runId).copy(
                finishedAtEpochMillis = finishedAtEpochMillis,
                distance = distance,
                movingDurationMillis = movingDurationMillis,
                elapsedDurationMillis = elapsedDurationMillis,
                accumulatedPausedMillis = accumulatedPausedMillis,
                isComplete = true,
            ),
        )
    }

    override suspend fun deleteRun(runId: Long) {
        state.value = state.value - runId
        points.remove(runId)
    }
}

class FakeAppPreferencesRepository(
    initial: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: Flow<AppPreferences> = state

    override suspend fun setTheme(theme: AppTheme) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        state.value = state.value.copy(unitSystem = unitSystem)
    }

    override suspend fun setAudioCuesEnabled(enabled: Boolean) {
        state.value = state.value.copy(audioCuesEnabled = enabled)
    }

    override suspend fun setCueIntervalUnits(units: Double) {
        state.value = state.value.copy(cueIntervalUnits = units)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        state.value = state.value.copy(hasCompletedOnboarding = completed)
    }

    override suspend fun setBatteryAdviceDismissed(dismissed: Boolean) {
        state.value = state.value.copy(batteryAdviceDismissed = dismissed)
    }
}
