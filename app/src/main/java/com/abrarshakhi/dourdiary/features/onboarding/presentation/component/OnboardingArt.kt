package com.abrarshakhi.dourdiary.features.onboarding.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedRouteArt(modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
        label = "routeDraw",
    )
    LaunchedEffect(Unit) { started = true }

    val routeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val headColor = MaterialTheme.colorScheme.tertiary

    val pathMeasure = remember { PathMeasure() }
    val drawn = remember { Path() }

    Canvas(modifier = modifier) {
        val full = loopPath(size)
        pathMeasure.setPath(full, false)

        drawPath(
            path = full,
            color = trackColor,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        drawn.reset()
        pathMeasure.getSegment(0f, pathMeasure.length * progress, drawn, true)
        drawPath(
            path = drawn,
            color = routeColor,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (progress > 0.01f) {
            val head = pathMeasure.getPosition(pathMeasure.length * progress)
            drawCircle(color = headColor, radius = 9.dp.toPx(), center = head)
            drawCircle(color = routeColor, radius = 4.dp.toPx(), center = head)
        }
    }
}

private fun loopPath(size: Size): Path {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radiusX = size.width * 0.34f
    val radiusY = size.height * 0.30f

    return Path().apply {
        val steps = 220
        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val angle = t * 2f * PI.toFloat()
            val wobble = 1f + 0.16f * sin(angle * 3f) + 0.07f * cos(angle * 5f)
            val x = centerX + cos(angle) * radiusX * wobble
            val y = centerY + sin(angle) * radiusY * wobble
            if (step == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

@Composable
fun LocationPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val primary = MaterialTheme.colorScheme.primary

    val waves = List(3) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2_400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = androidx.compose.animation.core.StartOffset(index * 800),
            ),
            label = "wave$index",
        )
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) / 2f

            waves.forEach { wave ->
                val fraction = wave.value
                drawCircle(
                    color = primary.copy(alpha = (1f - fraction) * 0.45f),
                    radius = maxRadius * fraction,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }

            drawCircle(color = primary, radius = 10.dp.toPx(), center = center)
            drawCircle(
                color = primary.copy(alpha = 0.25f),
                radius = 20.dp.toPx(),
                center = center,
            )
        }
    }
}
