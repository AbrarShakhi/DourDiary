package com.abrarshakhi.dourdiary.features.history.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HistoryViewModel(
    runRepository: RunRepository,
    appPreferencesRepository: AppPreferencesRepository,
) : MviViewModel<HistoryState, HistoryEvent, HistoryEffect>(HistoryState()) {

    init {
        runRepository.observeCompletedRuns()
            .onEach { runs -> setState { copy(runs = runs, isLoading = false) } }
            .launchIn(viewModelScope)

        runRepository.observeTotals()
            .onEach { totals -> setState { copy(totals = totals) } }
            .launchIn(viewModelScope)

        appPreferencesRepository.preferences
            .onEach { preferences -> setState { copy(unitSystem = preferences.unitSystem) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.RunClicked -> sendEffect(HistoryEffect.OpenRun(event.runId))
        }
    }
}
