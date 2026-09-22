package com.abrarshakhi.dourdiary.features.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.common.domain.model.DailyDistance
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekChart(
    days: List<DailyDistance>,
    busiestDistanceMeters: Double?,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val fraction = when {
                busiestDistanceMeters == null || busiestDistanceMeters <= 0.0 -> 0f
                else -> (day.distance.meters / busiestDistanceMeters).toFloat().coerceIn(0f, 1f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(BarColumnWidth),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(BarWidth),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(BarWidth)
                            .clip(RoundedCornerShape(BarCorner))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction.coerceAtLeast(MinimumVisibleFraction))
                                .width(BarWidth)
                                .clip(RoundedCornerShape(BarCorner))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                Text(
                    text = day.date.dayOfWeek
                        .getDisplayName(TextStyle.NARROW, locale)
                        .uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.date == today) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private val ChartHeight = 96.dp
private val BarColumnWidth = 32.dp
private val BarWidth = 10.dp
private val BarCorner = 5.dp
private const val MinimumVisibleFraction = 0.06f
