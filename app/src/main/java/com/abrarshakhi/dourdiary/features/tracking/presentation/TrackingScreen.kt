package com.abrarshakhi.dourdiary.features.tracking.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.common.presentation.map.MapCamera
import com.abrarshakhi.dourdiary.common.presentation.map.RouteMapRenderer
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.BatteryAdviceCard
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.LocationPermissionCard
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.RecoveredRunCard
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.RunControls
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.SecondaryStatRow

@Composable
fun TrackingScreen(
    state: TrackingState,
    onEvent: (TrackingEvent) -> Unit,
    routeMapRenderer: RouteMapRenderer,
    modifier: Modifier = Modifier,
) {
    val unitLabel = stringResource(
        when (state.unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kilometers_short
            UnitSystem.IMPERIAL -> R.string.unit_miles_short
        },
    )

    Box(modifier = modifier.fillMaxSize()) {
        routeMapRenderer.Map(
            points = state.route,
            camera = if (state.snapshot.status.isRecording) {
                MapCamera.FOLLOW_HEAD
            } else {
                MapCamera.FIT_ROUTE
            },
            modifier = Modifier.fillMaxSize(),
            showCurrentPosition = state.permission == LocationPermissionState.GRANTED,
            overlayBottomPadding = ControlsOverlayHeight,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.recoverableRun?.let { run ->
                RecoveredRunCard(
                    run = run,
                    unitSystem = state.unitSystem,
                    onResume = { onEvent(TrackingEvent.ResumeRecoveredRunClicked) },
                    onDiscard = { onEvent(TrackingEvent.DiscardRecoveredRunClicked) },
                )
            }

            if (state.permission != LocationPermissionState.GRANTED) {
                LocationPermissionCard(
                    permission = state.permission,
                    onGrantClicked = { onEvent(TrackingEvent.GrantPermissionClicked) },
                )
            }

            if (state.showBatteryAdvice) {
                BatteryAdviceCard(
                    onActionClicked = { onEvent(TrackingEvent.BatteryAdviceActionClicked) },
                    onDismissClicked = { onEvent(TrackingEvent.BatteryAdviceDismissed) },
                )
            }

            StatsPanel(state = state, unitLabel = unitLabel)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBanner(
                status = state.snapshot.status,
                hasPoorSignal = state.snapshot.hasPoorSignal,
            )
            RunControls(
                status = state.snapshot.status,
                enabled = state.canStart,
                onStart = { onEvent(TrackingEvent.StartClicked) },
                onPause = { onEvent(TrackingEvent.PauseClicked) },
                onResume = { onEvent(TrackingEvent.ResumeClicked) },
                onStop = { onEvent(TrackingEvent.StopClicked) },
            )
        }
    }
}

@Composable
private fun StatsPanel(
    state: TrackingState,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = PanelOpacity),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = state.snapshot.status == RunStatus.ACTIVE,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    RecordingDot(Modifier.padding(end = 10.dp))
                }
                DistanceHeadline(
                    value = RunFormatter.distance(state.snapshot.distance, state.unitSystem),
                    label = unitLabel,
                )
            }
            SecondaryStatRow(
                stats = listOf(
                    RunFormatter.duration(state.snapshot.movingDurationMillis) to
                        stringResource(R.string.tracking_duration),
                    RunFormatter.pace(state.snapshot.currentPace, state.unitSystem) to
                        stringResource(R.string.tracking_current_pace, unitLabel),
                    RunFormatter.pace(state.snapshot.averagePace, state.unitSystem) to
                        stringResource(R.string.tracking_average_pace, unitLabel),
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DistanceHeadline(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = value, style = MaterialTheme.typography.displayMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun StatusBanner(
    status: RunStatus,
    hasPoorSignal: Boolean,
    modifier: Modifier = Modifier,
) {
    val message = when {
        status == RunStatus.AUTO_PAUSED -> stringResource(R.string.tracking_auto_paused)
        status == RunStatus.PAUSED -> stringResource(R.string.tracking_paused)
        hasPoorSignal && status.isRecording -> stringResource(R.string.tracking_poor_signal)
        else -> null
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        StatusCard(message = message.orEmpty(), modifier = modifier)
    }
}

@Composable
private fun StatusCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RecordingDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "recording")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordingAlpha",
    )
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier.size(12.dp)) {
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2f)
    }
}

private const val PanelOpacity = 0.93f

private val ControlsOverlayHeight = 104.dp
