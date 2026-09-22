package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val restrictionChecker: BackgroundRestrictionChecker,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    init {
        refreshBatteryExemption()
        appPreferencesRepository.preferences
            .onEach { preferences ->
                setState {
                    copy(
                        theme = preferences.theme,
                        unitSystem = preferences.unitSystem,
                        audioCuesEnabled = preferences.audioCuesEnabled,
                        cueIntervalUnits = preferences.cueIntervalUnits,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> viewModelScope.launch {
                appPreferencesRepository.setTheme(event.theme)
            }

            is SettingsEvent.UnitSystemSelected -> viewModelScope.launch {
                appPreferencesRepository.setUnitSystem(event.unitSystem)
            }

            is SettingsEvent.AudioCuesToggled -> viewModelScope.launch {
                appPreferencesRepository.setAudioCuesEnabled(event.enabled)
            }

            is SettingsEvent.CueIntervalSelected -> viewModelScope.launch {
                appPreferencesRepository.setCueIntervalUnits(event.units)
            }

            SettingsEvent.ScreenResumed -> refreshBatteryExemption()

            SettingsEvent.BatteryOptimisationClicked ->
                sendEffect(SettingsEffect.OpenBatteryOptimisationSettings)
        }
    }

    private fun refreshBatteryExemption() {
        val exempt = restrictionChecker.isExemptFromBatteryOptimisation()
        setState { copy(batteryOptimisationExempt = exempt) }
    }
}
