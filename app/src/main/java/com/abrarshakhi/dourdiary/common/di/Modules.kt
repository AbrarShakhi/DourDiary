package com.abrarshakhi.dourdiary.common.di

import com.abrarshakhi.dourdiary.features.history.di.historyModule
import com.abrarshakhi.dourdiary.features.home.di.homeModule
import com.abrarshakhi.dourdiary.features.onboarding.di.onboardingModule
import com.abrarshakhi.dourdiary.features.settings.di.settingsModule
import com.abrarshakhi.dourdiary.features.summary.di.summaryModule
import com.abrarshakhi.dourdiary.features.tracking.di.trackingModule
import com.abrarshakhi.dourdiary.features.tracking.di.trackingPresentationModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    commonModule,
    databaseModule,
    trackingModule,
    trackingPresentationModule,
    settingsModule,
    historyModule,
    homeModule,
    onboardingModule,
    summaryModule,
)
