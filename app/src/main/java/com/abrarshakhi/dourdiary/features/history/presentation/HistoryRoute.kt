package com.abrarshakhi.dourdiary.features.history.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HistoryRoute(
    onRunSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
    thumbnailRenderer: RouteThumbnailRenderer = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            is HistoryEffect.OpenRun -> onRunSelected(effect.runId)
        }
    }

    HistoryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        thumbnailRenderer = thumbnailRenderer,
        modifier = modifier,
    )
}
