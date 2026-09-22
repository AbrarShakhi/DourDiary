package com.abrarshakhi.dourdiary.features.summary.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.presentation.map.RouteMapRenderer
import com.abrarshakhi.dourdiary.common.presentation.mvi.CollectEffects
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun RunSummaryRoute(
    runId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    routeMapRenderer: RouteMapRenderer = koinInject(),
) {
    val viewModel: RunSummaryViewModel = koinViewModel(
        key = "run-summary-$runId",
        parameters = { parametersOf(runId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            RunSummaryEffect.Close -> onDone()
        }
    }

    RunSummaryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        routeMapRenderer = routeMapRenderer,
        modifier = modifier,
    )
}
