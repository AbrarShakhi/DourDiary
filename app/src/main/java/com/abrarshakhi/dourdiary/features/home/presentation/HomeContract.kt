package com.abrarshakhi.dourdiary.features.home.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.domain.model.WeeklySummary
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState
import java.time.LocalDate

@Immutable
data class HomeState(
    val isLoading: Boolean = true,
    val week: WeeklySummary = WeeklySummary(),
    val recentRuns: List<Run> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val today: LocalDate? = null,
) : UiState

sealed interface HomeEvent : UiEvent {
    data object ScreenResumed : HomeEvent
    data class RunClicked(val runId: Long) : HomeEvent
    data object StartRunClicked : HomeEvent
}

sealed interface HomeEffect : UiEffect {
    data class OpenRun(val runId: Long) : HomeEffect
    data object OpenRecord : HomeEffect
}
