package com.abrarshakhi.dourdiary.features.history.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.RunTotals
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState

@Immutable
data class HistoryState(
    val isLoading: Boolean = true,
    val runs: List<Run> = emptyList(),
    val totals: RunTotals = RunTotals(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
) : UiState

sealed interface HistoryEvent : UiEvent {
    data class RunClicked(val runId: Long) : HistoryEvent
}

sealed interface HistoryEffect : UiEffect {
    data class OpenRun(val runId: Long) : HistoryEffect
}
