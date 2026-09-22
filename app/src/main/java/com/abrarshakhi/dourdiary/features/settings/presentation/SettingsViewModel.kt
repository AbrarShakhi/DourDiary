package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    init {
        appPreferencesRepository.preferences
            .onEach { preferences ->
                setState {
                    copy(theme = preferences.theme, dynamicColor = preferences.dynamicColor)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> viewModelScope.launch {
                appPreferencesRepository.setTheme(event.theme)
            }

            is SettingsEvent.DynamicColorToggled -> viewModelScope.launch {
                appPreferencesRepository.setDynamicColor(event.enabled)
            }
        }
    }
}
