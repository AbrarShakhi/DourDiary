package com.abrarshakhi.dourdiary.features.summary.di

import com.abrarshakhi.dourdiary.features.summary.presentation.RunSummaryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val summaryModule = module {
    viewModel { parameters ->
        RunSummaryViewModel(runId = parameters.get(), get(), get(), get())
    }
}
