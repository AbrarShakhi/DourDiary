package com.abrarshakhi.dourdiary.features.summary.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.common.presentation.map.MapCamera
import com.abrarshakhi.dourdiary.common.presentation.map.RouteMapRenderer
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.PrimaryStat
import com.abrarshakhi.dourdiary.features.tracking.presentation.component.SecondaryStatRow

@Composable
fun RunSummaryScreen(
    state: RunSummaryState,
    onEvent: (RunSummaryEvent) -> Unit,
    routeMapRenderer: RouteMapRenderer,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val run = state.run
    if (run == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.summary_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val unitLabel = stringResource(
        when (state.unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kilometers_short
            UnitSystem.IMPERIAL -> R.string.unit_miles_short
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = RunFormatter.dateTime(run.startedAtEpochMillis),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )

        PrimaryStat(
            value = RunFormatter.distance(run.distance, state.unitSystem),
            label = unitLabel,
        )

        SecondaryStatRow(
            stats = listOf(
                RunFormatter.duration(run.movingDurationMillis) to
                    stringResource(R.string.tracking_duration),
                RunFormatter.pace(run.averagePace, state.unitSystem) to
                    stringResource(R.string.tracking_average_pace, unitLabel),
                RunFormatter.duration(run.elapsedDurationMillis) to
                    stringResource(R.string.summary_elapsed),
            ),
        )

        if (state.route.size >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                routeMapRenderer.Map(
                    points = state.route,
                    camera = MapCamera.FIT_ROUTE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RouteMapHeight),
                    showCurrentPosition = false,
                    overlayBottomPadding = 0.dp,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.summary_no_route),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(
            onClick = { onEvent(RunSummaryEvent.DeleteClicked) },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.summary_delete),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { onEvent(RunSummaryEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.summary_delete_title)) },
            text = { Text(stringResource(R.string.summary_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(RunSummaryEvent.DeleteConfirmed) }) {
                    Text(
                        text = stringResource(R.string.summary_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(RunSummaryEvent.DeleteDismissed) }) {
                    Text(stringResource(R.string.summary_delete_cancel))
                }
            },
        )
    }
}

private val RouteMapHeight = 280.dp
