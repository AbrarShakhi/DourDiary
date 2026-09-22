package com.abrarshakhi.dourdiary.features.summary.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.res.stringResource
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.main.ScreenChrome
import com.abrarshakhi.dourdiary.common.navigation.back

@OptIn(ExperimentalMaterial3Api::class)
fun runSummaryChrome(): ScreenChrome = ScreenChrome(
    topBar = { scope ->
        TopAppBar(
            title = { Text(stringResource(R.string.summary_title)) },
            navigationIcon = {
                IconButton(onClick = { scope.backStack.back() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            scrollBehavior = scope.scrollBehavior,
        )
    },
)
