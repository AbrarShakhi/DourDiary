package com.abrarshakhi.dourdiary.features.onboarding.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        viewModel.onEvent(
            OnboardingEvent.PermissionResult(
                granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            ),
        )
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(OnboardingEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            OnboardingEffect.RequestLocationPermission -> permissionLauncher.launch(
                buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray(),
            )

            OnboardingEffect.Finished -> onFinished()
        }
    }

    OnboardingScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
