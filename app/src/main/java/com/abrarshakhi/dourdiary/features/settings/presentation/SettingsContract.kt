package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState

@Immutable
data class SettingsState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
) : UiState

sealed interface SettingsEvent : UiEvent {
    data class ThemeSelected(val theme: AppTheme) : SettingsEvent
    data class DynamicColorToggled(val enabled: Boolean) : SettingsEvent
}

sealed interface SettingsEffect : UiEffect
