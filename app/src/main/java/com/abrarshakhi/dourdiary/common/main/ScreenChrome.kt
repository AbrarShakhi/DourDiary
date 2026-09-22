package com.abrarshakhi.dourdiary.common.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.abrarshakhi.dourdiary.common.navigation.AppRouteKey
import com.abrarshakhi.dourdiary.common.navigation.BottomBarKey
import com.abrarshakhi.dourdiary.features.history.presentation.historyChrome
import com.abrarshakhi.dourdiary.features.home.presentation.homeChrome
import com.abrarshakhi.dourdiary.features.onboarding.presentation.onboardingChrome
import com.abrarshakhi.dourdiary.features.settings.presentation.licensesChrome
import com.abrarshakhi.dourdiary.features.settings.presentation.settingsChrome
import com.abrarshakhi.dourdiary.features.summary.presentation.runSummaryChrome
import com.abrarshakhi.dourdiary.features.tracking.presentation.trackingChrome

@OptIn(ExperimentalMaterial3Api::class)
@Immutable
data class ChromeScope(
    val backStack: SnapshotStateList<AppRouteKey>,
    val scrollBehavior: TopAppBarScrollBehavior,
)

@Immutable
data class ScreenChrome(
    val topBar: @Composable (ChromeScope) -> Unit = {},
    val fab: @Composable (ChromeScope) -> Unit = {},
    val bottomBar: BottomBarKey? = null,
    val contentWindowInsets: WindowInsets? = null,
)

fun AppRouteKey.chrome(): ScreenChrome = when (this) {
    is AppRouteKey.Onboarding -> onboardingChrome()
    is AppRouteKey.Home -> homeChrome()
    is AppRouteKey.Record -> trackingChrome()
    is AppRouteKey.History -> historyChrome()
    is AppRouteKey.Settings -> settingsChrome()
    is AppRouteKey.Licenses -> licensesChrome()
    is AppRouteKey.RunSummary -> runSummaryChrome()
}
