package com.abrarshakhi.dourdiary.features.home.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.common.domain.stats.WeeklySummaryCalculator
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val runRepository: RunRepository,
    appPreferencesRepository: AppPreferencesRepository,
    private val clock: Clock,
    private val locale: Locale = Locale.getDefault(),
) : MviViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {
    private val weekStart = MutableStateFlow(currentWeekStart())

    init {
        weekStart
            .flatMapLatest { start ->
                val startEpoch = start.atStartOfDay(clock.zone).toInstant().toEpochMilli()
                runRepository.observeRunsSince(startEpoch)
            }
            .onEach { runs ->
                val today = LocalDate.now(clock)
                val summary = WeeklySummaryCalculator.summarise(
                    runs = runs,
                    today = today,
                    firstDayOfWeek = firstDayOfWeek(),
                    zone = clock.zone,
                )
                setState { copy(week = summary, today = today, isLoading = false) }
            }
            .launchIn(viewModelScope)

        runRepository.observeRecentRuns(RecentRunCount)
            .onEach { runs -> setState { copy(recentRuns = runs, isLoading = false) } }
            .launchIn(viewModelScope)

        appPreferencesRepository.preferences
            .onEach { preferences -> setState { copy(unitSystem = preferences.unitSystem) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.ScreenResumed -> {
                setState { copy(today = LocalDate.now(clock)) }
                weekStart.value = currentWeekStart()
            }
            is HomeEvent.RunClicked -> sendEffect(HomeEffect.OpenRun(event.runId))
            HomeEvent.StartRunClicked -> sendEffect(HomeEffect.OpenRecord)
        }
    }

    private fun firstDayOfWeek() = WeekFields.of(locale).firstDayOfWeek

    private fun currentWeekStart(): LocalDate =
        WeeklySummaryCalculator.startOfWeek(LocalDate.now(clock), firstDayOfWeek())

    private companion object {
        const val RecentRunCount = 10
    }
}
