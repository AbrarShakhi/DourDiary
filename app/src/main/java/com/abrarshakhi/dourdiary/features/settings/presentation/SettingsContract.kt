package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState

@Immutable
data class SettingsState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val audioCuesEnabled: Boolean = true,
    val cueIntervalUnits: Double = 1.0,
    val batteryOptimisationExempt: Boolean = true,
) : UiState {
    val cueIntervalOptions: List<Double> get() = listOf(0.5, 1.0, 2.0, 5.0)
}

sealed interface SettingsEvent : UiEvent {
    data class ThemeSelected(val theme: AppTheme) : SettingsEvent
    data class UnitSystemSelected(val unitSystem: UnitSystem) : SettingsEvent
    data class AudioCuesToggled(val enabled: Boolean) : SettingsEvent
    data class CueIntervalSelected(val units: Double) : SettingsEvent

    data object ScreenResumed : SettingsEvent
    data object BatteryOptimisationClicked : SettingsEvent
}

sealed interface SettingsEffect : UiEffect {
    data object OpenBatteryOptimisationSettings : SettingsEffect
}
