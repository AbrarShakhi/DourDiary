package com.abrarshakhi.dourdiary.features.home.di

import com.abrarshakhi.dourdiary.features.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(runRepository = get(), appPreferencesRepository = get(), clock = get()) }
}
