package com.abrarshakhi.dourdiary.common.presentation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint

class CanvasRouteThumbnailRenderer : RouteThumbnailRenderer {

    @Composable
    override fun Thumbnail(points: List<GeoPoint>, routeId: String, modifier: Modifier) {
        val colorScheme = MaterialTheme.colorScheme
        val projected = remember(points) { RouteProjection.project(points) }
        val hasExtent = remember(points) { RouteProjection.hasExtent(points) }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            if (!hasExtent) {
                Text(
                    text = stringResource(R.string.tracking_route_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                return@Box
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 12.dp.toPx()
                val width = (size.width - inset * 2).coerceAtLeast(1f)
                val height = (size.height - inset * 2).coerceAtLeast(1f)
                val side = minOf(width, height)
                val offsetX = inset + (width - side) / 2f
                val offsetY = inset + (height - side) / 2f

                fun place(point: NormalizedPoint) =
                    Offset(offsetX + point.x * side, offsetY + point.y * side)

                val path = Path().apply {
                    projected.forEachIndexed { index, point ->
                        val position = place(point)
                        if (index == 0) moveTo(position.x, position.y)
                        else lineTo(position.x, position.y)
                    }
                }

                drawPath(
                    path = path,
                    color = colorScheme.primary,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                drawCircle(
                    color = colorScheme.tertiary,
                    radius = 4.dp.toPx(),
                    center = place(projected.first()),
                )

            }
        }
    }
}
