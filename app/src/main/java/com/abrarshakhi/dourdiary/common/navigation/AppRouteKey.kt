package com.abrarshakhi.dourdiary.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRouteKey : NavKey {

    @Serializable
    data object Onboarding : AppRouteKey

    @Serializable
    data object Home : AppRouteKey, BottomBarKey

    @Serializable
    data object History : AppRouteKey, BottomBarKey

    @Serializable
    data object Settings : AppRouteKey, BottomBarKey
}
