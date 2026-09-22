package com.abrarshakhi.dourdiary.features.tracking.presentation

import androidx.compose.foundation.layout.WindowInsets
import com.abrarshakhi.dourdiary.common.main.ScreenChrome
import com.abrarshakhi.dourdiary.common.navigation.AppRouteKey

fun trackingChrome(): ScreenChrome = ScreenChrome(
    bottomBar = AppRouteKey.Record,
    contentWindowInsets = WindowInsets(0),
)
