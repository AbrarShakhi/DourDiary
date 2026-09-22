package com.abrarshakhi.dourdiary.features.summary.presentation

import androidx.compose.runtime.Immutable
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEffect
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiEvent
import com.abrarshakhi.dourdiary.common.presentation.mvi.UiState

@Immutable
data class RunSummaryState(
    val isLoading: Boolean = true,
    val run: Run? = null,
    val route: List<GeoPoint> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val confirmingDelete: Boolean = false,
) : UiState

sealed interface RunSummaryEvent : UiEvent {
    data object DeleteClicked : RunSummaryEvent
    data object DeleteConfirmed : RunSummaryEvent
    data object DeleteDismissed : RunSummaryEvent
}

sealed interface RunSummaryEffect : UiEffect {
    data object Close : RunSummaryEffect
}
