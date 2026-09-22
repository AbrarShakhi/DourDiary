package com.abrarshakhi.dourdiary.features.summary.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.common.presentation.map.thumbnail.RouteThumbnailCache
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RunSummaryViewModel(
    private val runId: Long,
    private val runRepository: RunRepository,
    private val thumbnailCache: RouteThumbnailCache,
    appPreferencesRepository: AppPreferencesRepository,
) : MviViewModel<RunSummaryState, RunSummaryEvent, RunSummaryEffect>(RunSummaryState()) {

    init {
        viewModelScope.launch {
            val run = runRepository.getRun(runId)
            setState { copy(run = run, isLoading = false) }
        }

        runRepository.observeRoute(runId)
            .onEach { route -> setState { copy(route = route.map { it.point }) } }
            .launchIn(viewModelScope)

        appPreferencesRepository.preferences
            .onEach { preferences -> setState { copy(unitSystem = preferences.unitSystem) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: RunSummaryEvent) {
        when (event) {
            RunSummaryEvent.DeleteClicked -> setState { copy(confirmingDelete = true) }

            RunSummaryEvent.DeleteDismissed -> setState { copy(confirmingDelete = false) }

            RunSummaryEvent.DeleteConfirmed -> viewModelScope.launch {
                runRepository.deleteRun(runId)
                thumbnailCache.evictRoute(runId.toString())
                setState { copy(confirmingDelete = false) }
                sendEffect(RunSummaryEffect.Close)
            }
        }
    }
}
