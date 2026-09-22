package com.abrarshakhi.dourdiary.common.navigation

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
import com.abrarshakhi.dourdiary.features.settings.presentation.SettingsRoute

@Composable
fun AppNavigation(
    backStack: SnapshotStateList<AppRouteKey>,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRouteKey.Onboarding> { OnboardingRoute() }
            entry<AppRouteKey.Home> { HomeRoute() }
            entry<AppRouteKey.History> { HistoryRoute() }
            entry<AppRouteKey.Settings> { SettingsRoute() }
        },
    )
}
