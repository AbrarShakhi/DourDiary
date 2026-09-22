package com.abrarshakhi.dourdiary.features.onboarding.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState

@Immutable
data class OnboardingState(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val locationGranted: Boolean = false,
) : UiState

sealed interface OnboardingEvent : UiEvent {
    data object ScreenResumed : OnboardingEvent
    data class UnitSystemSelected(val unitSystem: UnitSystem) : OnboardingEvent
    data object GrantLocationClicked : OnboardingEvent
    data class PermissionResult(val granted: Boolean) : OnboardingEvent
    data object FinishClicked : OnboardingEvent
    data object SkipClicked : OnboardingEvent
}

sealed interface OnboardingEffect : UiEffect {
    data object RequestLocationPermission : OnboardingEffect
    data object Finished : OnboardingEffect
}
