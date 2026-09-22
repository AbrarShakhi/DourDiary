package com.abrarshakhi.dourdiary.features.tracking.presentation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.map.RouteMapRenderer
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import com.abrarshakhi.dourdiary.common.presentation.system.openAppDetailsSettings
import com.abrarshakhi.dourdiary.common.presentation.system.openBatteryOptimisationSettings
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun TrackingRoute(
    onRunFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = koinViewModel(),
    routeMapRenderer: RouteMapRenderer = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val activity = context as? Activity
        val canAskAgain = granted || activity == null || ActivityCompat
            .shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)

        viewModel.onEvent(TrackingEvent.PermissionResult(granted = granted, canAskAgain = canAskAgain))
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(TrackingEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            TrackingEffect.RequestLocationPermission -> permissionLauncher.launch(
                buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray(),
            )

            is TrackingEffect.OpenRunSummary -> onRunFinished(effect.runId)

            TrackingEffect.OpenAppSettings -> context.openAppDetailsSettings()

            TrackingEffect.OpenBatteryOptimisationSettings ->
                context.openBatteryOptimisationSettings()
        }
    }

    TrackingScreen(
        state = state,
        onEvent = viewModel::onEvent,
        routeMapRenderer = routeMapRenderer,
        modifier = modifier,
    )
}
