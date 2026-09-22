package com.abrarshakhi.dourdiary.features.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeRoute(
    onRunSelected: (Long) -> Unit,
    onStartRunClicked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    thumbnailRenderer: RouteThumbnailRenderer = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(HomeEvent.ScreenResumed)
        onPauseOrDispose { }
    }

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is HomeEffect.OpenRun -> onRunSelected(effect.runId)
            HomeEffect.OpenRecord -> onStartRunClicked()
        }
    }

    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        thumbnailRenderer = thumbnailRenderer,
        modifier = modifier,
    )
}
