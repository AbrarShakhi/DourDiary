package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunState
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunEngineTest {

    private val config = TrackingConfig()
    private val engine = RunEngine(config)

    private val startEpoch = 1_700_000_000_000L

    private fun startedRun() = engine.start(nowElapsedRealtimeMillis = 0L, nowEpochMillis = startEpoch)

    private fun replay(state: RunState, samples: List<LocationSample>): RunState =
        samples.fold(state) { current, sample -> engine.onLocation(current, sample).state }

    @Test
    fun `an idle run has no duration`() {
        val idle = RunState()

        assertEquals(0L, idle.movingDurationMillis(999_999L))
        assertEquals(0L, idle.elapsedDurationMillis(999_999L))
    }

    @Test
    fun `duration is derived from the start instant, not accumulated`() {
        val state = startedRun()

        assertEquals(60_000L, state.movingDurationMillis(60_000L))
    }

    @Test
    fun `manual pause stops the moving clock but not the elapsed clock`() {
        val paused = engine.pause(startedRun(), nowElapsedRealtimeMillis = 30_000L)

        assertEquals(30_000L, paused.movingDurationMillis(90_000L))
        assertEquals(90_000L, paused.elapsedDurationMillis(90_000L))
    }

    @Test
    fun `resuming adds the pause to the ledger`() {
        var state = startedRun()
        state = engine.pause(state, 30_000L)
        state = engine.resume(state, 100_000L)

        assertEquals(70_000L, state.accumulatedPausedMillis)
        assertEquals(60_000L, state.movingDurationMillis(130_000L))
    }

    @Test
    fun `stopping freezes both clocks`() {
        var state = startedRun()
        state = engine.stop(state, nowElapsedRealtimeMillis = 120_000L)

        assertEquals(RunStatus.FINISHED, state.status)
        assertEquals(120_000L, state.movingDurationMillis(999_999L))
        assertEquals(120_000L, state.elapsedDurationMillis(999_999L))
    }

    @Test
    fun `stopping while paused does not double count the pause`() {
        var state = startedRun()
        state = engine.pause(state, 30_000L)
        state = engine.stop(state, 50_000L)

        assertEquals(30_000L, state.movingDurationMillis(999_999L))
    }

    @Test
    fun `pausing an already auto-paused run keeps the original pause instant`() {
        val autoPaused = startedRun().copy(
            status = RunStatus.AUTO_PAUSED,
            pausedAtElapsedRealtimeMillis = 20_000L,
        )

        val paused = engine.pause(autoPaused, nowElapsedRealtimeMillis = 50_000L)

        assertEquals(20_000L, paused.pausedAtElapsedRealtimeMillis)
        assertEquals(20_000L, paused.movingDurationMillis(50_000L))
    }

    @Test
    fun `a steady run accumulates the distance it covered`() {
        val track = RunTrackBuilder().move(seconds = 300, metersPerSecond = 3.0)

        val state = replay(startedRun(), track.build())

        assertEquals(900.0, state.distance.meters, 1.0)
    }

    @Test
    fun `the first fix anchors without adding distance`() {
        val track = RunTrackBuilder()

        val state = replay(startedRun(), track.build())

        assertEquals(0.0, state.distance.meters, 1e-9)
        assertNotNull(state.lastPoint)
    }

    @Test
    fun `standing still with drifting GPS adds no distance`() {
        val track = RunTrackBuilder().drift(seconds = 120, amplitudeMeters = 1.5)

        val state = replay(startedRun(), track.build())

        assertEquals(
            "GPS drift became phantom distance",
            0.0,
            state.distance.meters,
            1e-9,
        )
    }

    @Test
    fun `a GPS teleport is discarded instead of inflating distance`() {
        val track = RunTrackBuilder()
            .move(seconds = 60, metersPerSecond = 3.0)
            .jump(meters = 5_000.0)
            .move(seconds = 60, metersPerSecond = 3.0)

        val state = replay(startedRun(), track.build())

        assertTrue(
            "A GPS jump leaked into the distance: ${state.distance.meters}",
            state.distance.meters < 400.0,
        )
    }

    @Test
    fun `fixes too inaccurate to trust are ignored`() {
        val track = RunTrackBuilder().loseSignal(seconds = 60, metersPerSecond = 3.0)

        val state = replay(startedRun(), track.build())

        assertEquals(0.0, state.distance.meters, 1e-9)
        assertEquals(RejectionReason.POOR_ACCURACY, state.lastRejection)
    }

    @Test
    fun `slow movement below the displacement floor is batched, not lost`() {
        val track = RunTrackBuilder().move(seconds = 120, metersPerSecond = 1.0)

        val state = replay(startedRun(), track.build())

        assertEquals(120.0, state.distance.meters, 4.0)
    }

    @Test
    fun `the run pauses itself when the runner stops and resumes when they go again`() {
        val track = RunTrackBuilder()
            .move(seconds = 60, metersPerSecond = 3.0)
            .standStill(seconds = 30)
            .move(seconds = 60, metersPerSecond = 3.0)

        val state = replay(startedRun(), track.build())

        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(25_000L, state.accumulatedPausedMillis)
        assertEquals(125_000L, state.movingDurationMillis(track.elapsedMillis))
    }

    @Test
    fun `time spent auto-paused is excluded from the moving clock`() {
        val track = RunTrackBuilder()
            .move(seconds = 30, metersPerSecond = 3.0)
            .standStill(seconds = 300)

        val state = replay(startedRun(), track.build())

        assertEquals(RunStatus.AUTO_PAUSED, state.status)
        assertEquals(39_000L, state.movingDurationMillis(track.elapsedMillis))
    }

    @Test
    fun `the leg spanning a pause is not counted as distance covered`() {
        val track = RunTrackBuilder()
            .move(seconds = 30, metersPerSecond = 3.0)
            .standStill(seconds = 60)
            .move(seconds = 30, metersPerSecond = 3.0)

        val state = replay(startedRun(), track.build())

        assertTrue(
            "Distance ${state.distance.meters} includes the paused leg",
            state.distance.meters < 190.0,
        )
    }

    @Test
    fun `disabling auto pause keeps the clock running through a stop`() {
        val noAutoPause = RunEngine(config.copy(autoPauseEnabled = false))
        val track = RunTrackBuilder()
            .move(seconds = 30, metersPerSecond = 3.0)
            .standStill(seconds = 120)

        val state = track.build().fold(
            noAutoPause.start(0L, startEpoch),
        ) { current, sample -> noAutoPause.onLocation(current, sample).state }

        assertEquals(RunStatus.ACTIVE, state.status)
        assertEquals(0L, state.accumulatedPausedMillis)
        assertEquals(150_000L, state.movingDurationMillis(track.elapsedMillis))
    }

    @Test
    fun `a manually paused run ignores location entirely`() {
        var state = replay(startedRun(), RunTrackBuilder().move(60, 3.0).build())
        val distanceAtPause = state.distance

        state = engine.pause(state, 60_000L)
        val whilePaused = RunTrackBuilder().move(seconds = 60, metersPerSecond = 3.0).build()
        state = replay(state, whilePaused)

        assertEquals(RunStatus.PAUSED, state.status)
        assertEquals(distanceAtPause, state.distance)
    }

    @Test
    fun `only the runner can resume a manually paused run`() {
        var state = engine.pause(startedRun(), 10_000L)

        state = replay(state, RunTrackBuilder().move(60, 3.0).build())

        assertEquals(RunStatus.PAUSED, state.status)
    }

    @Test
    fun `a finished run records nothing further`() {
        var state = replay(startedRun(), RunTrackBuilder().move(60, 3.0).build())
        state = engine.stop(state, 60_000L)
        val finalDistance = state.distance

        state = replay(state, RunTrackBuilder().move(60, 3.0).build())

        assertEquals(finalDistance, state.distance)
    }

    @Test
    fun `snapshot reports the pace actually run`() {
        val track = RunTrackBuilder().move(seconds = 300, metersPerSecond = 3.0)
        val state = replay(startedRun(), track.build())

        val snapshot = engine.snapshot(state, track.elapsedMillis)

        assertEquals(1_000.0 / 3.0, snapshot.averagePace!!.secondsPerKilometer, 1.0)
        assertEquals(1_000.0 / 3.0, snapshot.currentPace!!.secondsPerKilometer, 1.0)
    }

    @Test
    fun `a paused run shows no current pace`() {
        val track = RunTrackBuilder()
            .move(seconds = 30, metersPerSecond = 3.0)
            .standStill(seconds = 60)
        val state = replay(startedRun(), track.build())

        val snapshot = engine.snapshot(state, track.elapsedMillis)

        assertEquals(RunStatus.AUTO_PAUSED, snapshot.status)
        assertNull(snapshot.currentPace)
    }

    @Test
    fun `poor signal is surfaced to the UI`() {
        val track = RunTrackBuilder().loseSignal(seconds = 10)
        val state = replay(startedRun(), track.build())

        assertTrue(engine.snapshot(state, track.elapsedMillis).hasPoorSignal)
    }

    @Test
    fun `recovering signal clears the warning`() {
        val track = RunTrackBuilder()
            .loseSignal(seconds = 10)
            .move(seconds = 10, metersPerSecond = 3.0)
        val state = replay(startedRun(), track.build())

        assertTrue(engine.snapshot(state, track.elapsedMillis).hasPoorSignal.not())
    }

    @Test
    fun `every accepted fix is handed back for persistence`() {
        val samples = RunTrackBuilder().move(seconds = 60, metersPerSecond = 3.0).build()

        var state = startedRun()
        val recorded = samples.mapNotNull { sample ->
            val update = engine.onLocation(state, sample)
            state = update.state
            update.recordedPoint
        }

        assertEquals(61, recorded.size)
        assertEquals(state.distance, recorded.last().cumulativeDistance)
    }

    @Test
    fun `recorded points carry a monotonically increasing cumulative distance`() {
        val samples = RunTrackBuilder()
            .move(60, 3.0)
            .standStill(30)
            .move(60, 3.0)
            .build()

        var state = startedRun()
        val recorded = samples.mapNotNull { sample ->
            val update = engine.onLocation(state, sample)
            state = update.state
            update.recordedPoint
        }

        recorded.zipWithNext { earlier, later ->
            assertTrue(
                "Cumulative distance went backwards",
                later.cumulativeDistance >= earlier.cumulativeDistance,
            )
        }
    }
}
