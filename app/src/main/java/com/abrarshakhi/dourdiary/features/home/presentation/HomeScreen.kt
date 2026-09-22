package com.abrarshakhi.dourdiary.features.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.component.RunFeedCard
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.ui.theme.StatNumber
import com.abrarshakhi.dourdiary.features.home.presentation.component.WeekChart

@Composable
fun HomeScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    thumbnailRenderer: RouteThumbnailRenderer,
    modifier: Modifier = Modifier,
) {
    val unitLabel = stringResource(
        when (state.unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kilometers_short
            UnitSystem.IMPERIAL -> R.string.unit_miles_short
        },
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WeekCard(
                state = state,
                unitLabel = unitLabel,
            )
        }

        if (state.recentRuns.isEmpty()) {
            item { EmptyFeedCard(onStartRun = { onEvent(HomeEvent.StartRunClicked) }) }
        } else {
            item {
                Text(
                    text = stringResource(R.string.home_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(items = state.recentRuns, key = { it.id }) { run ->
                RunFeedCard(
                    run = run,
                    unitSystem = state.unitSystem,
                    thumbnailRenderer = thumbnailRenderer,
                    onClick = { onEvent(HomeEvent.RunClicked(run.id)) },
                )
            }
        }
    }
}

@Composable
private fun WeekCard(
    state: HomeState,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_this_week).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = RunFormatter.distance(state.week.totalDistance, state.unitSystem),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                )
            }

            WeekChart(
                days = state.week.days,
                busiestDistanceMeters = state.week.busiestDayDistance?.meters,
                today = state.today,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                WeekStat(
                    value = state.week.runCount.toString(),
                    label = stringResource(R.string.home_stat_runs),
                )
                WeekStat(
                    value = RunFormatter.duration(state.week.totalMovingDurationMillis),
                    label = stringResource(R.string.home_stat_time),
                )
                WeekStat(
                    value = RunFormatter.pace(state.week.averagePace, state.unitSystem),
                    label = stringResource(R.string.tracking_average_pace, unitLabel),
                )
            }
        }
    }
}

@Composable
private fun WeekStat(value: String, label: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleLarge.merge(StatNumber))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyFeedCard(onStartRun: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_no_runs_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_no_runs_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            Button(onClick = onStartRun) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.home_start_run),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
