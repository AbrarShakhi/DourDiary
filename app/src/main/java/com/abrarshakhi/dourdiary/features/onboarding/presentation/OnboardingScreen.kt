package com.abrarshakhi.dourdiary.features.onboarding.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.features.onboarding.presentation.component.AnimatedRouteArt
import com.abrarshakhi.dourdiary.features.onboarding.presentation.component.LocationPulse
import com.abrarshakhi.dourdiary.features.onboarding.presentation.component.PageIndicator
import kotlinx.coroutines.launch

private const val PageCount = 4

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PageCount })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            AnimatedVisibility(visible = pagerState.currentPage < PageCount - 1) {
                TextButton(onClick = { onEvent(OnboardingEvent.SkipClicked) }) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_welcome_title),
                    body = stringResource(R.string.onboarding_welcome_body),
                    art = { AnimatedRouteArt(Modifier.fillMaxSize()) },
                )

                1 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_privacy_title),
                    body = stringResource(R.string.onboarding_privacy_body),
                    art = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(96.dp),
                        )
                    },
                )

                2 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_location_title),
                    body = stringResource(R.string.onboarding_location_body),
                    art = { LocationPulse(Modifier.fillMaxSize()) },
                ) {
                    LocationAction(
                        granted = state.locationGranted,
                        onGrantClicked = { onEvent(OnboardingEvent.GrantLocationClicked) },
                    )
                }

                else -> OnboardingPage(
                    title = stringResource(R.string.onboarding_units_title),
                    body = stringResource(R.string.onboarding_units_body),
                    art = {
                        Text(
                            text = if (state.unitSystem == UnitSystem.METRIC) "km" else "mi",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    UnitChoice(
                        selected = state.unitSystem,
                        onSelected = { onEvent(OnboardingEvent.UnitSystemSelected(it)) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PageIndicator(pageCount = PageCount, currentPage = pagerState.currentPage)

            val isLastPage = pagerState.currentPage == PageCount - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onEvent(OnboardingEvent.FinishClicked)
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                    ),
                )
                Icon(
                    imageVector = if (isLastPage) {
                        Icons.Filled.Check
                    } else {
                        Icons.AutoMirrored.Filled.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    title: String,
    body: String,
    art: @Composable () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            art()
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 150)) +
                slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    initialOffsetY = { it / 3 },
                ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionSlotHeight)
                .padding(top = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            action?.invoke()
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LocationAction(granted: Boolean, onGrantClicked: () -> Unit) {
    if (granted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_location_granted),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    } else {
        Button(onClick = onGrantClicked) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

@Composable
private fun UnitChoice(selected: UnitSystem, onSelected: (UnitSystem) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UnitSystem.entries.forEach { unitSystem ->
            FilterChip(
                selected = unitSystem == selected,
                onClick = { onSelected(unitSystem) },
                label = {
                    Text(
                        stringResource(
                            when (unitSystem) {
                                UnitSystem.METRIC -> R.string.settings_units_metric
                                UnitSystem.IMPERIAL -> R.string.settings_units_imperial
                            },
                        ),
                    )
                },
            )
        }
    }
}

private val ActionSlotHeight = 88.dp
