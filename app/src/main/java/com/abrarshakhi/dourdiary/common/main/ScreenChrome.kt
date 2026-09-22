package com.abrarshakhi.dourdiary.common.main

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
import com.abrarshakhi.dourdiary.features.settings.presentation.settingsChrome

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
)

fun AppRouteKey.chrome(): ScreenChrome = when (this) {
    AppRouteKey.Onboarding -> onboardingChrome()
    AppRouteKey.Home -> homeChrome()
    AppRouteKey.History -> historyChrome()
    AppRouteKey.Settings -> settingsChrome()
}
