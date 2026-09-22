package com.abrarshakhi.dourdiary.features.tracking.data.session

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.RunEngine
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunSessionManagerTest {

    private val config = TrackingConfig()
    private val clock = FakeTrackingClock()
    private val locations = FakeLocationDataSource()
    private val repository = FakeRunRepository()
    private val preferences = FakeAppPreferencesRepository()

    private fun TestScope.manager() = RunSessionManager(
        locationDataSource = locations,
        runRepository = repository,
        preferencesRepository = preferences,
        clock = clock,
        engine = RunEngine(config),
        scope = backgroundScope,
        config = config,
    )

    private val metersPerDegreeLatitude = 111_195.08

    private fun fixAt(meters: Double, atMillis: Long, speed: Double? = 3.0): LocationSample {
        clock.elapsed = atMillis
        return LocationSample(
            point = GeoPoint(52.52 + meters / metersPerDegreeLatitude, 13.405),
            accuracyMeters = 5.0,
            elapsedRealtimeMillis = atMillis,
            epochMillis = 1_700_000_000_000L + atMillis,
            speedMetersPerSecond = speed,
        )
    }

    @Test
    fun starting_creates_a_run_row_immediately_so_a_crash_leaves_something_behind() = runTest {
        val manager = manager()

        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        assertEquals(1, repository.runs.size)
        assertEquals(1L, manager.activeRunId.value)
        assertTrue(repository.runs.getValue(1L).isComplete.not())
    }

    @Test
    fun starting_twice_does_not_create_a_second_run() = runTest {
        val manager = manager()

        manager.start()
        advanceTimeBy(10L)
        runCurrent()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        assertEquals(1, repository.runs.size)
    }

    @Test
    fun accepted_fixes_reach_the_database_in_batches() = runTest {
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        repeat(25) { second ->
            locations.fixes.emit(fixAt(meters = second * 3.0, atMillis = second * 1_000L))
            runCurrent()
        }

        val stored = repository.points[1L].orEmpty()
        assertTrue("Nothing was persisted", stored.isNotEmpty())
        assertTrue(
            "Wrote once per fix instead of batching: ${repository.appendCallCount} calls",
            repository.appendCallCount < 25,
        )
    }

    @Test
    fun progress_is_checkpointed_while_the_run_is_in_flight() = runTest {
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        repeat(30) { second ->
            locations.fixes.emit(fixAt(meters = second * 3.0, atMillis = second * 1_000L))
            runCurrent()
        }

        assertTrue(
            "The in-progress run was never checkpointed",
            repository.progressUpdateCount > 0,
        )
        assertTrue(repository.runs.getValue(1L).distance.meters > 0.0)
    }

    @Test
    fun the_live_snapshot_tracks_the_distance_covered() = runTest {
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        repeat(61) { second ->
            locations.fixes.emit(fixAt(meters = second * 3.0, atMillis = second * 1_000L))
            runCurrent()
        }

        assertEquals(180.0, manager.snapshot.value.distance.meters, 1.0)
        assertEquals(RunStatus.ACTIVE, manager.snapshot.value.status)
    }

    @Test
    fun stopping_completes_the_run_and_flushes_what_was_pending() = runTest {
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()
        repeat(5) { second ->
            locations.fixes.emit(fixAt(meters = second * 3.0, atMillis = second * 1_000L))
            runCurrent()
        }

        clock.elapsed = 5_000L
        manager.stop()
        advanceTimeBy(10L)
        runCurrent()

        val saved = repository.runs.getValue(1L)
        assertTrue("The run was not marked complete", saved.isComplete)
        assertNotNull(saved.finishedAtEpochMillis)
        assertEquals(5, repository.points.getValue(1L).size)
        assertNull(manager.activeRunId.value)
    }

    @Test
    fun pausing_and_resuming_keeps_the_paused_time_out_of_the_moving_clock() = runTest {
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        clock.elapsed = 30_000L
        manager.pause()
        runCurrent()
        assertEquals(RunStatus.PAUSED, manager.snapshot.value.status)

        clock.elapsed = 90_000L
        manager.resume()
        runCurrent()

        clock.elapsed = 120_000L
        manager.pause()
        runCurrent()

        assertEquals(60_000L, manager.snapshot.value.movingDurationMillis)
    }

    @Test
    fun a_run_left_by_a_crash_is_offered_back() = runTest {
        repository.put(abandonedRun())
        val manager = manager()

        manager.checkForRecoverableRun()

        assertEquals(1L, manager.recoverableRun.value?.id)
    }

    @Test
    fun nothing_is_offered_when_every_run_finished_cleanly() = runTest {
        repository.put(abandonedRun().copy(isComplete = true))
        val manager = manager()

        manager.checkForRecoverableRun()

        assertNull(manager.recoverableRun.value)
    }

    @Test
    fun a_recovered_run_comes_back_with_its_distance_and_clock_intact() = runTest {
        repository.put(abandonedRun())
        val manager = manager()
        manager.checkForRecoverableRun()

        clock.elapsed = 9_000_000L
        manager.resumeRecoveredRun()
        advanceTimeBy(10L)
        runCurrent()

        val snapshot = manager.snapshot.value
        assertEquals(18_000.0, snapshot.distance.meters, 1e-9)
        assertEquals(5_400_000L, snapshot.movingDurationMillis)
        assertEquals(1L, manager.activeRunId.value)
        assertEquals(RunStatus.PAUSED, snapshot.status)
        assertNull(manager.recoverableRun.value)
    }

    @Test
    fun the_gap_while_the_app_was_dead_is_not_counted_as_distance() = runTest {
        repository.put(abandonedRun())
        val manager = manager()
        manager.checkForRecoverableRun()
        clock.elapsed = 9_000_000L
        manager.resumeRecoveredRun()
        advanceTimeBy(10L)
        runCurrent()

        manager.resume()
        runCurrent()

        locations.fixes.emit(fixAt(meters = 100_000.0, atMillis = 9_001_000L))
        runCurrent()
        locations.fixes.emit(fixAt(meters = 100_003.0, atMillis = 9_002_000L))
        runCurrent()

        assertEquals(
            "The dead time became distance",
            18_003.0,
            manager.snapshot.value.distance.meters,
            1.0,
        )
    }

    @Test
    fun discarding_a_recovered_run_files_it_with_what_it_managed_to_record() = runTest {
        repository.put(abandonedRun())
        val manager = manager()
        manager.checkForRecoverableRun()

        manager.discardRecoveredRun()

        val saved = repository.runs.getValue(1L)
        assertTrue(saved.isComplete)
        assertEquals(18_000.0, saved.distance.meters, 1e-9)
        assertNull(manager.recoverableRun.value)
    }

    @Test
    fun a_run_in_progress_hides_any_recovery_prompt() = runTest {
        repository.put(abandonedRun().copy(id = 99L))
        val manager = manager()
        manager.start()
        advanceTimeBy(10L)
        runCurrent()

        manager.checkForRecoverableRun()

        assertNull(manager.recoverableRun.value)
    }

    private fun abandonedRun() = Run(
        id = 1L,
        startedAtEpochMillis = 1_700_000_000_000L,
        finishedAtEpochMillis = null,
        distance = Distance(18_000.0),
        movingDurationMillis = 5_400_000L,
        elapsedDurationMillis = 5_500_000L,
        accumulatedPausedMillis = 100_000L,
        isComplete = false,
    )
}
