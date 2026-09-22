package com.abrarshakhi.dourdiary.features.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val permissionChecker: LocationPermissionChecker,
) : MviViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(OnboardingState()) {

    init {
        appPreferencesRepository.preferences
            .onEach { preferences -> setState { copy(unitSystem = preferences.unitSystem) } }
            .launchIn(viewModelScope)

        refreshPermission()
    }

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.ScreenResumed -> refreshPermission()

            is OnboardingEvent.UnitSystemSelected -> viewModelScope.launch {
                appPreferencesRepository.setUnitSystem(event.unitSystem)
            }

            OnboardingEvent.GrantLocationClicked ->
                sendEffect(OnboardingEffect.RequestLocationPermission)

            is OnboardingEvent.PermissionResult ->
                setState { copy(locationGranted = event.granted) }

            OnboardingEvent.FinishClicked, OnboardingEvent.SkipClicked -> complete()
        }
    }

    private fun complete() {
        viewModelScope.launch {
            appPreferencesRepository.setOnboardingCompleted(true)
            sendEffect(OnboardingEffect.Finished)
        }
    }

    private fun refreshPermission() {
        setState { copy(locationGranted = permissionChecker.hasLocationPermission()) }
    }
}
