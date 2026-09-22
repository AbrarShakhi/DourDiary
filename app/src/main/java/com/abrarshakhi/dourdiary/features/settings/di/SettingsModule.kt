package com.abrarshakhi.dourdiary.features.settings.di

import com.abrarshakhi.dourdiary.features.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}
