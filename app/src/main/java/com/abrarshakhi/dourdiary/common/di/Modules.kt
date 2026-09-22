package com.abrarshakhi.dourdiary.common.di

import com.abrarshakhi.dourdiary.features.settings.di.settingsModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    commonModule,
    settingsModule,
)
