package com.abrarshakhi.dourdiary.common.domain.repository

import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {

    val preferences: Flow<AppPreferences>

    suspend fun setTheme(theme: AppTheme)

    suspend fun setUnitSystem(unitSystem: UnitSystem)

    suspend fun setAudioCuesEnabled(enabled: Boolean)

    suspend fun setCueIntervalUnits(units: Double)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setBatteryAdviceDismissed(dismissed: Boolean)
}
