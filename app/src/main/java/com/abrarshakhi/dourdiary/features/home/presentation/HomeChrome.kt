package com.abrarshakhi.dourdiary.features.home.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.main.ScreenChrome
import com.abrarshakhi.dourdiary.common.navigation.AppRouteKey

@OptIn(ExperimentalMaterial3Api::class)
fun homeChrome(): ScreenChrome = ScreenChrome(
    topBar = { scope ->
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            scrollBehavior = scope.scrollBehavior,
        )
    },
    bottomBar = AppRouteKey.Home,
)
