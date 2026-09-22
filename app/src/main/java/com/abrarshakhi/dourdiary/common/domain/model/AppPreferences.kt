package com.abrarshakhi.dourdiary.common.domain.model

data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val audioCuesEnabled: Boolean = true,
    val cueIntervalUnits: Double = 1.0,
    val hasCompletedOnboarding: Boolean = false,
    val batteryAdviceDismissed: Boolean = false,
)
