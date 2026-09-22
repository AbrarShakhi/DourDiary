package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Speed
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoPauseDetectorTest {

    private val config = TrackingConfig(
        autoPauseSpeedThresholdMetersPerSecond = 0.5,
        autoPauseEnterAfterMillis = 8_000L,
        autoPauseExitAfterMillis = 3_000L,
    )
    private val detector = AutoPauseDetector(config)

    private val running = Speed(3.0)
    private val standingStill = Speed(0.1)

    private fun hold(
        from: AutoPauseState,
        speed: Speed,
        seconds: Int,
        startMillis: Long,
    ): AutoPauseState {
        var state = from
        for (second in 0 until seconds) {
            state = detector.next(state, speed, startMillis + second * 1_000L)
        }
        return state
    }

    @Test
    fun `does not pause before the threshold has been held long enough`() {
        val state = hold(AutoPauseState(), standingStill, seconds = 7, startMillis = 0L)

        assertFalse("Paused after only 6 seconds of stillness", state.paused)
    }

    @Test
    fun `pauses once stationary for the full enter delay`() {
        val state = hold(AutoPauseState(), standingStill, seconds = 10, startMillis = 0L)

        assertTrue(state.paused)
    }

    @Test
    fun `a single stationary fix while running does not pause`() {
        val moving = hold(AutoPauseState(), running, seconds = 30, startMillis = 0L)

        val blip = detector.next(moving, standingStill, 30_000L)

        assertFalse("One stale fix paused the run", blip.paused)
    }

    @Test
    fun `resumes after sustained movement`() {
        val paused = hold(AutoPauseState(), standingStill, seconds = 10, startMillis = 0L)
        assertTrue(paused.paused)

        val resumed = hold(paused, running, seconds = 5, startMillis = 20_000L)

        assertTrue(resumed.paused.not())
    }

    @Test
    fun `a single moving fix does not resume a paused run`() {
        val paused = hold(AutoPauseState(), standingStill, seconds = 10, startMillis = 0L)

        val blip = detector.next(paused, running, 20_000L)

        assertTrue("One GPS wobble resumed the run", blip.paused)
    }

    @Test
    fun `interrupted stillness restarts the enter countdown`() {
        var state = hold(AutoPauseState(), standingStill, seconds = 6, startMillis = 0L)
        state = detector.next(state, running, 6_000L)
        state = hold(state, standingStill, seconds = 6, startMillis = 7_000L)

        assertFalse(state.paused)
    }

    @Test
    fun `speed exactly at the threshold counts as moving`() {
        val state = hold(AutoPauseState(), Speed(0.5), seconds = 20, startMillis = 0L)

        assertFalse(state.paused)
    }

    @Test
    fun `disabling auto pause never pauses`() {
        val disabled = AutoPauseDetector(config.copy(autoPauseEnabled = false))

        var state = AutoPauseState()
        repeat(60) { second -> state = disabled.next(state, standingStill, second * 1_000L) }

        assertFalse(state.paused)
    }

    @Test
    fun `irregular fix intervals still measure real elapsed time`() {
        var state = detector.next(AutoPauseState(), standingStill, 0L)
        state = detector.next(state, standingStill, 10_000L)

        assertTrue(state.paused)
    }
}
