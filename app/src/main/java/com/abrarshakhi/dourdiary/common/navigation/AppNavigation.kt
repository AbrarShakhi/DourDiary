package com.abrarshakhi.dourdiary.common.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.abrarshakhi.dourdiary.features.history.presentation.HistoryRoute
import com.abrarshakhi.dourdiary.features.home.presentation.HomeRoute
import com.abrarshakhi.dourdiary.features.onboarding.presentation.OnboardingRoute
import com.abrarshakhi.dourdiary.features.summary.presentation.RunSummaryRoute
import com.abrarshakhi.dourdiary.features.tracking.presentation.TrackingRoute
import com.abrarshakhi.dourdiary.features.settings.presentation.LicensesScreen
import com.abrarshakhi.dourdiary.features.settings.presentation.SettingsRoute

private val SlideSpec = spring<androidx.compose.ui.unit.IntOffset>(
    stiffness = Spring.StiffnessMediumLow,
    dampingRatio = Spring.DampingRatioNoBouncy,
)

@Composable
fun AppNavigation(
    backStack: SnapshotStateList<AppRouteKey>,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.back() },
        transitionSpec = {
            (slideInHorizontally(SlideSpec) { it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally(SlideSpec) { -it / 6 } + fadeOut())
        },
        popTransitionSpec = {
            (slideInHorizontally(SlideSpec) { -it / 6 } + fadeIn()) togetherWith
                (slideOutHorizontally(SlideSpec) { it / 4 } + fadeOut())
        },
        predictivePopTransitionSpec = {
            (slideInHorizontally(SlideSpec) { -it / 6 } + fadeIn()) togetherWith
                (slideOutHorizontally(SlideSpec) { it / 4 } + fadeOut())
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRouteKey.Onboarding> {
                OnboardingRoute(
                    onFinished = { backStack.switchTabTo(AppRouteKey.Home) },
                )
            }
            entry<AppRouteKey.Home> {
                HomeRoute(
                    onRunSelected = { runId -> backStack.navigateTo(AppRouteKey.RunSummary(runId)) },
                    onStartRunClicked = { backStack.switchTabTo(AppRouteKey.Record) },
                )
            }
            entry<AppRouteKey.Record> {
                TrackingRoute(
                    onRunFinished = { runId -> backStack.navigateTo(AppRouteKey.RunSummary(runId)) },
                )
            }
            entry<AppRouteKey.History> {
                HistoryRoute(onRunSelected = { runId -> backStack.navigateTo(AppRouteKey.RunSummary(runId)) })
            }
            entry<AppRouteKey.Settings> {
                SettingsRoute(onOpenLicenses = { backStack.navigateTo(AppRouteKey.Licenses) })
            }
            entry<AppRouteKey.Licenses> { LicensesScreen() }
            entry<AppRouteKey.RunSummary> { key ->
                RunSummaryRoute(runId = key.runId, onDone = { backStack.back() })
            }
        },
    )
}
