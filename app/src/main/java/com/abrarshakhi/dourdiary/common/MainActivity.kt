package com.abrarshakhi.dourdiary.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.dourdiary.common.main.AppRoot
import com.abrarshakhi.dourdiary.common.main.MainAppViewModel
import com.abrarshakhi.dourdiary.common.ui.theme.DourDiaryTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainAppViewModel: MainAppViewModel = koinViewModel()
            val preferences by mainAppViewModel.preferences.collectAsStateWithLifecycle()

            DourDiaryTheme(
                appTheme = preferences.theme,
                dynamicColor = preferences.dynamicColor,
            ) {
                AppRoot()
            }
        }
    }
}
