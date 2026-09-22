package com.abrarshakhi.dourdiary.features.tracking.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.presentation.mvi.MviViewModel
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker
import com.abrarshakhi.dourdiary.features.tracking.domain.repository.RunTracker
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TrackingViewModel(
    private val runTracker: RunTracker,
    private val permissionChecker: LocationPermissionChecker,
    private val restrictionChecker: BackgroundRestrictionChecker,
    private val appPreferencesRepository: AppPreferencesRepository,
) : MviViewModel<TrackingState, TrackingEvent, TrackingEffect>(TrackingState()) {

    init {
        runTracker.snapshot
            .onEach { snapshot -> setState { copy(snapshot = snapshot) } }
            .launchIn(viewModelScope)

        runTracker.route
            .onEach { route -> setState { copy(route = route) } }
            .launchIn(viewModelScope)

        runTracker.recoverableRun
            .onEach { run -> setState { copy(recoverableRun = run) } }
            .launchIn(viewModelScope)

        runTracker.finishedRuns
            .onEach { runId -> sendEffect(TrackingEffect.OpenRunSummary(runId)) }
            .launchIn(viewModelScope)

        appPreferencesRepository.preferences
            .onEach { preferences ->
                setState {
                    copy(
                        unitSystem = preferences.unitSystem,
                        batteryAdviceDismissed = preferences.batteryAdviceDismissed,
                    )
                }
            }
            .launchIn(viewModelScope)

        refreshPermission()
        refreshPowerRestriction()
        viewModelScope.launch { runTracker.checkForRecoverableRun() }
    }

    override fun onEvent(event: TrackingEvent) {
        when (event) {
            TrackingEvent.ScreenResumed -> {
                refreshPermission()
                refreshPowerRestriction()
                viewModelScope.launch { runTracker.checkForRecoverableRun() }
            }

            TrackingEvent.StartClicked -> if (currentState.canStart) {
                runTracker.start()
            } else {
                sendEffect(TrackingEffect.RequestLocationPermission)
            }

            TrackingEvent.PauseClicked -> runTracker.pause()

            TrackingEvent.ResumeClicked -> runTracker.resume()

            TrackingEvent.StopClicked -> runTracker.stop()

            TrackingEvent.GrantPermissionClicked -> {
                if (currentState.permission == LocationPermissionState.PERMANENTLY_DENIED) {
                    sendEffect(TrackingEffect.OpenAppSettings)
                } else {
                    sendEffect(TrackingEffect.RequestLocationPermission)
                }
            }

            is TrackingEvent.PermissionResult -> setState {
                copy(
                    permission = when {
                        event.granted -> LocationPermissionState.GRANTED
                        event.canAskAgain -> LocationPermissionState.DENIED
                        else -> LocationPermissionState.PERMANENTLY_DENIED
                    },
                )
            }

            TrackingEvent.ResumeRecoveredRunClicked -> runTracker.resumeRecoveredRun()

            TrackingEvent.DiscardRecoveredRunClicked -> viewModelScope.launch {
                runTracker.discardRecoveredRun()
            }

            TrackingEvent.BatteryAdviceActionClicked ->
                sendEffect(TrackingEffect.OpenBatteryOptimisationSettings)

            TrackingEvent.BatteryAdviceDismissed -> viewModelScope.launch {
                appPreferencesRepository.setBatteryAdviceDismissed(true)
            }
        }
    }

    private fun refreshPermission() {
        val granted = permissionChecker.hasLocationPermission()
        setState {
            copy(
                permission = when {
                    granted -> LocationPermissionState.GRANTED
                    permission == LocationPermissionState.PERMANENTLY_DENIED -> permission
                    else -> LocationPermissionState.UNKNOWN
                },
            )
        }
    }

    private fun refreshPowerRestriction() {
        val exempt = restrictionChecker.isExemptFromBatteryOptimisation()
        setState { copy(powerRestricted = !exempt) }
    }
}
