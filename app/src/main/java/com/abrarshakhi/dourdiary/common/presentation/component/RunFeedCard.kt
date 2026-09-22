package com.abrarshakhi.dourdiary.common.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.ui.theme.StatNumber

@Composable
fun RunFeedCard(
    run: Run,
    unitSystem: UnitSystem,
    thumbnailRenderer: RouteThumbnailRenderer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember(run.id) { mutableStateOf(false) }
    LaunchedEffect(run.id) { visible = true }
    val unitLabel = stringResource(
        when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kilometers_short
            UnitSystem.IMPERIAL -> R.string.unit_miles_short
        },
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 6 },
        ),
    ) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Text(
            text = RunFormatter.dateTime(run.startedAtEpochMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
        )

        if (run.simplifiedRoute.size >= 2) {
            thumbnailRenderer.Thumbnail(
                points = run.simplifiedRoute,
                routeId = run.id.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .height(ThumbnailHeight),
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CardStat(
                value = RunFormatter.distance(run.distance, unitSystem),
                label = unitLabel,
            )
            CardStat(
                value = RunFormatter.duration(run.movingDurationMillis),
                label = stringResource(R.string.tracking_duration),
            )
            CardStat(
                value = RunFormatter.pace(run.averagePace, unitSystem),
                label = stringResource(R.string.tracking_average_pace, unitLabel),
            )
        }
    }
}
}

@Composable
private fun CardStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.merge(StatNumber),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ThumbnailHeight = 140.dp
