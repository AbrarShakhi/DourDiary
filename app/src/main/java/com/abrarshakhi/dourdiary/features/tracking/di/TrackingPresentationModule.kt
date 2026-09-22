package com.abrarshakhi.dourdiary.features.tracking.di

import com.abrarshakhi.dourdiary.features.tracking.presentation.TrackingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val trackingPresentationModule = module {
    viewModelOf(::TrackingViewModel)
}
