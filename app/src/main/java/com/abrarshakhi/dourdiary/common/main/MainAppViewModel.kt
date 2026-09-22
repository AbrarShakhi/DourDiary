package com.abrarshakhi.dourdiary.common.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

class MainAppViewModel(
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<AppPreferences?> = appPreferencesRepository.preferences.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS.milliseconds),
            initialValue = null,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
