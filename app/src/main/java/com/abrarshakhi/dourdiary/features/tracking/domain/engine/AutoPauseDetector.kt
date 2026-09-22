package com.abrarshakhi.dourdiary.features.tracking.domain.engine

import com.abrarshakhi.dourdiary.common.domain.model.Speed
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig

data class AutoPauseState(
    val paused: Boolean = false,
    val belowThresholdSinceMillis: Long? = null,
    val aboveThresholdSinceMillis: Long? = null,
)

class AutoPauseDetector(private val config: TrackingConfig) {

    fun next(state: AutoPauseState, speed: Speed, elapsedRealtimeMillis: Long): AutoPauseState {
        if (!config.autoPauseEnabled) return AutoPauseState()

        val isStationary = speed.metersPerSecond < config.autoPauseSpeedThresholdMetersPerSecond

        return if (isStationary) {
            val since = state.belowThresholdSinceMillis ?: elapsedRealtimeMillis
            val heldFor = elapsedRealtimeMillis - since
            AutoPauseState(
                paused = state.paused || heldFor >= config.autoPauseEnterAfterMillis,
                belowThresholdSinceMillis = since,
                aboveThresholdSinceMillis = null,
            )
        } else {
            val since = state.aboveThresholdSinceMillis ?: elapsedRealtimeMillis
            val heldFor = elapsedRealtimeMillis - since
            AutoPauseState(
                paused = state.paused && heldFor < config.autoPauseExitAfterMillis,
                belowThresholdSinceMillis = null,
                aboveThresholdSinceMillis = since,
            )
        }
    }
}
