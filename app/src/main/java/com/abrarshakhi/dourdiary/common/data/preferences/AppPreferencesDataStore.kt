package com.abrarshakhi.dourdiary.common.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class AppPreferencesDataStore(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {

    override val preferences: Flow<AppPreferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
        else throw throwable
    }.map { stored ->
        val defaults = AppPreferences()
        AppPreferences(
            theme = stored[KeyTheme]?.toAppTheme() ?: defaults.theme,
            unitSystem = stored[KeyUnitSystem]?.toUnitSystem() ?: defaults.unitSystem,
            audioCuesEnabled = stored[KeyAudioCues] ?: defaults.audioCuesEnabled,
            cueIntervalUnits = stored[KeyCueInterval] ?: defaults.cueIntervalUnits,
            hasCompletedOnboarding = stored[KeyOnboardingDone] ?: defaults.hasCompletedOnboarding,
            batteryAdviceDismissed = stored[KeyBatteryAdviceDismissed]
                ?: defaults.batteryAdviceDismissed,
        )
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KeyTheme] = theme.name }
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        dataStore.edit { it[KeyUnitSystem] = unitSystem.name }
    }

    override suspend fun setAudioCuesEnabled(enabled: Boolean) {
        dataStore.edit { it[KeyAudioCues] = enabled }
    }

    override suspend fun setCueIntervalUnits(units: Double) {
        require(units > 0.0) { "Cue interval must be positive, was $units" }
        dataStore.edit { it[KeyCueInterval] = units }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KeyOnboardingDone] = completed }
    }

    override suspend fun setBatteryAdviceDismissed(dismissed: Boolean) {
        dataStore.edit { it[KeyBatteryAdviceDismissed] = dismissed }
    }

    private fun String.toAppTheme(): AppTheme? = AppTheme.entries.firstOrNull { it.name == this }

    private fun String.toUnitSystem(): UnitSystem? =
        UnitSystem.entries.firstOrNull { it.name == this }

    private companion object {
        val KeyTheme = stringPreferencesKey("theme")
        val KeyUnitSystem = stringPreferencesKey("unit_system")
        val KeyAudioCues = booleanPreferencesKey("audio_cues_enabled")
        val KeyCueInterval = doublePreferencesKey("cue_interval_units")
        val KeyOnboardingDone = booleanPreferencesKey("onboarding_completed")
        val KeyBatteryAdviceDismissed = booleanPreferencesKey("battery_advice_dismissed")
    }
}
