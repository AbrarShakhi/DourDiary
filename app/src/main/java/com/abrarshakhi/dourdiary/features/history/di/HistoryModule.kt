package com.abrarshakhi.dourdiary.features.history.di

import com.abrarshakhi.dourdiary.features.history.presentation.HistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val historyModule = module {
    viewModelOf(::HistoryViewModel)
}
