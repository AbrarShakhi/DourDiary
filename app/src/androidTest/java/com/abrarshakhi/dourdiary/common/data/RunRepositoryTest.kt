package com.abrarshakhi.dourdiary.common.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abrarshakhi.dourdiary.common.data.database.DourDiaryDatabase
import com.abrarshakhi.dourdiary.common.data.repository.RoomRunRepository
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.TrackPoint
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunRepositoryTest {

    private lateinit var database: DourDiaryDatabase
    private lateinit var repository: RunRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DourDiaryDatabase::class.java,
        ).build()

        repository = RoomRunRepository(
            runDao = database.runDao(),
            runPointDao = database.runPointDao(),
            ioDispatcher = Dispatchers.IO,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun trackPoint(
        index: Int,
        cumulativeMeters: Double,
    ) = TrackPoint(
        point = GeoPoint(52.52 + index * 0.0001, 13.405, altitudeMeters = 34.0 + index),
        elapsedRealtimeMillis = index * 1_000L,
        epochMillis = 1_700_000_000_000L + index * 1_000L,
        cumulativeDistance = Distance(cumulativeMeters),
    )

    @Test
    fun a_started_run_is_discoverable_as_in_progress() = runTest {
        val runId = repository.startRun(startedAtEpochMillis = 1_700_000_000_000L)

        val inProgress = repository.findInProgressRun()

        assertNotNull("A started run was not recoverable", inProgress)
        assertEquals(runId, inProgress!!.id)
        assertTrue(inProgress.isComplete.not())
    }

    @Test
    fun a_completed_run_is_no_longer_in_progress() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)

        repository.completeRun(
            runId = runId,
            finishedAtEpochMillis = 1_700_000_600_000L,
            distance = Distance(2_400.0),
            movingDurationMillis = 600_000L,
            elapsedDurationMillis = 640_000L,
            accumulatedPausedMillis = 40_000L,
        )

        assertNull(repository.findInProgressRun())
        val saved = repository.getRun(runId)!!
        assertTrue(saved.isComplete)
        assertEquals(2_400.0, saved.distance.meters, 1e-9)
        assertEquals(600_000L, saved.movingDurationMillis)
        assertEquals(40_000L, saved.accumulatedPausedMillis)
    }

    @Test
    fun progress_checkpoints_survive_so_a_crash_cannot_lose_the_run() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)

        repository.updateProgress(
            runId = runId,
            distance = Distance(18_000.0),
            movingDurationMillis = 5_400_000L,
            elapsedDurationMillis = 5_500_000L,
            accumulatedPausedMillis = 100_000L,
        )

        val recovered = repository.findInProgressRun()!!
        assertEquals(18_000.0, recovered.distance.meters, 1e-9)
        assertEquals(5_400_000L, recovered.movingDurationMillis)
        assertEquals(100_000L, recovered.accumulatedPausedMillis)
    }

    @Test
    fun route_points_round_trip_in_recorded_order() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)
        val points = (0..9).map { trackPoint(it, cumulativeMeters = it * 3.0) }

        repository.appendPoints(runId, points)

        val stored = repository.getRoute(runId)
        assertEquals(10, stored.size)
        assertEquals(points.map { it.elapsedRealtimeMillis }, stored.map { it.elapsedRealtimeMillis })
        assertEquals(27.0, stored.last().cumulativeDistance.meters, 1e-9)
        assertEquals(43.0, stored.last().point.altitudeMeters!!, 1e-9)
    }

    @Test
    fun appending_in_batches_preserves_a_single_ordered_route() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)

        repository.appendPoints(runId, (0..4).map { trackPoint(it, it * 3.0) })
        repository.appendPoints(runId, (5..9).map { trackPoint(it, it * 3.0) })

        val stored = repository.observeRoute(runId).first()
        assertEquals(10, stored.size)
        stored.zipWithNext { earlier, later ->
            assertTrue(earlier.elapsedRealtimeMillis < later.elapsedRealtimeMillis)
        }
    }

    @Test
    fun appending_nothing_is_a_no_op() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)

        repository.appendPoints(runId, emptyList())

        assertEquals(0, repository.getRoute(runId).size)
    }

    @Test
    fun history_lists_completed_runs_newest_first() = runTest {
        val older = repository.startRun(1_700_000_000_000L)
        val newer = repository.startRun(1_700_000_900_000L)
        repository.completeRun(older, 1_700_000_300_000L, Distance(1_000.0), 300_000L, 300_000L, 0L)
        repository.completeRun(newer, 1_700_001_200_000L, Distance(2_000.0), 300_000L, 300_000L, 0L)

        val runs = repository.observeCompletedRuns().first()

        assertEquals(listOf(newer, older), runs.map { it.id })
    }

    @Test
    fun history_excludes_a_run_that_is_still_recording() = runTest {
        val finished = repository.startRun(1_700_000_000_000L)
        repository.startRun(1_700_000_900_000L)
        repository.completeRun(finished, 1_700_000_300_000L, Distance(1_000.0), 300_000L, 300_000L, 0L)

        val runs = repository.observeCompletedRuns().first()

        assertEquals(listOf(finished), runs.map { it.id })
    }

    @Test
    fun totals_aggregate_only_completed_runs() = runTest {
        val first = repository.startRun(1_700_000_000_000L)
        val second = repository.startRun(1_700_000_900_000L)
        val abandoned = repository.startRun(1_700_001_900_000L)
        repository.completeRun(first, 1L, Distance(5_000.0), 1_500_000L, 1_500_000L, 0L)
        repository.completeRun(second, 2L, Distance(10_000.0), 3_000_000L, 3_000_000L, 0L)
        repository.updateProgress(abandoned, Distance(999_000.0), 1L, 1L, 0L)

        val totals = repository.observeTotals().first()

        assertEquals(2, totals.runCount)
        assertEquals(15_000.0, totals.totalDistance.meters, 1e-9)
        assertEquals(4_500_000L, totals.totalMovingDurationMillis)
    }

    @Test
    fun totals_of_an_empty_history_are_zero_not_null() = runTest {
        val totals = repository.observeTotals().first()

        assertEquals(0, totals.runCount)
        assertEquals(0.0, totals.totalDistance.meters, 1e-9)
        assertNull(totals.averagePace)
    }

    @Test
    fun deleting_a_run_takes_its_route_with_it() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)
        repository.appendPoints(runId, (0..9).map { trackPoint(it, it * 3.0) })

        repository.deleteRun(runId)

        assertNull(repository.getRun(runId))
        assertEquals(
            "Route points outlived their run",
            0,
            database.runPointDao().countForRun(runId),
        )
    }

    @Test
    fun average_pace_is_derived_from_the_stored_figures() = runTest {
        val runId = repository.startRun(1_700_000_000_000L)
        repository.completeRun(
            runId = runId,
            finishedAtEpochMillis = 2L,
            distance = Distance(10_000.0),
            movingDurationMillis = 50 * 60 * 1_000L,
            elapsedDurationMillis = 50 * 60 * 1_000L,
            accumulatedPausedMillis = 0L,
        )

        val saved = repository.getRun(runId)!!

        assertEquals(300.0, saved.averagePace!!.secondsPerKilometer, 1e-6)
    }

    @Test
    fun an_unknown_run_reads_back_as_absent() = runTest {
        assertNull(repository.getRun(404L))
        assertEquals(emptyList<TrackPoint>(), repository.getRoute(404L))
    }
}
