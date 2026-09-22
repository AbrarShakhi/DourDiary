package com.abrarshakhi.dourdiary.features.onboarding.di

import com.abrarshakhi.dourdiary.features.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
