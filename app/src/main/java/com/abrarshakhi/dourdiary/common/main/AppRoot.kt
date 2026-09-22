package com.abrarshakhi.dourdiary.common.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.abrarshakhi.dourdiary.common.navigation.AppNavigation
import com.abrarshakhi.dourdiary.common.navigation.AppRouteKey
import com.abrarshakhi.dourdiary.common.navigation.BottomBar
import com.abrarshakhi.dourdiary.common.navigation.currentRoute
import com.abrarshakhi.dourdiary.common.navigation.rememberAppBackStack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(startRoute: AppRouteKey = AppRouteKey.Home) {
    val backStack = rememberAppBackStack(startRoute)
    val current = backStack.currentRoute() ?: startRoute
    val chrome = current.chrome()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val chromeScope = remember(backStack, scrollBehavior) {
        ChromeScope(backStack = backStack, scrollBehavior = scrollBehavior)
    }

    LaunchedEffect(current) {
        scrollBehavior.state.contentOffset = 0f
        scrollBehavior.state.heightOffset = 0f
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { chrome.topBar(chromeScope) },
        floatingActionButton = { chrome.fab(chromeScope) },
        bottomBar = {
            chrome.bottomBar?.let { selected ->
                BottomBar(selected = selected, backStack = backStack)
            }
        },
    ) { innerPadding ->
        AppNavigation(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
