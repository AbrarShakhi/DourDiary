package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import com.abrarshakhi.dourdiary.common.presentation.system.openBatteryOptimisationSettings
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(SettingsEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            SettingsEffect.OpenBatteryOptimisationSettings ->
                context.openBatteryOptimisationSettings()
        }
    }

    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenLicenses = onOpenLicenses,
        modifier = modifier,
    )
}
