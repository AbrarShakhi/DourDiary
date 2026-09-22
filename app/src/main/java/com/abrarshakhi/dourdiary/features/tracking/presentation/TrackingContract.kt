package com.abrarshakhi.dourdiary.features.tracking.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot

enum class LocationPermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

@Immutable
data class TrackingState(
    val snapshot: RunSnapshot = RunSnapshot(),
    val route: List<GeoPoint> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val permission: LocationPermissionState = LocationPermissionState.UNKNOWN,
    val recoverableRun: Run? = null,
    val powerRestricted: Boolean = false,
    val batteryAdviceDismissed: Boolean = false,
) : UiState {

    val canStart: Boolean get() = permission == LocationPermissionState.GRANTED

    val showBatteryAdvice: Boolean
        get() = powerRestricted && !batteryAdviceDismissed && !snapshot.status.isRecording
}

sealed interface TrackingEvent : UiEvent {
    data object ScreenResumed : TrackingEvent

    data object StartClicked : TrackingEvent
    data object PauseClicked : TrackingEvent
    data object ResumeClicked : TrackingEvent
    data object StopClicked : TrackingEvent

    data object GrantPermissionClicked : TrackingEvent
    data class PermissionResult(
        val granted: Boolean,
        val canAskAgain: Boolean,
    ) : TrackingEvent

    data object ResumeRecoveredRunClicked : TrackingEvent
    data object DiscardRecoveredRunClicked : TrackingEvent

    data object BatteryAdviceActionClicked : TrackingEvent
    data object BatteryAdviceDismissed : TrackingEvent
}

sealed interface TrackingEffect : UiEffect {
    data object RequestLocationPermission : TrackingEffect
    data object OpenAppSettings : TrackingEffect
    data object OpenBatteryOptimisationSettings : TrackingEffect
    data class OpenRunSummary(val runId: Long) : TrackingEffect
}
