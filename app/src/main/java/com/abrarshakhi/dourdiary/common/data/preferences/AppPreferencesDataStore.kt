package com.abrarshakhi.dourdiary.common.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
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
        AppPreferences(
            theme = stored[KeyTheme]?.toAppTheme() ?: AppPreferences().theme,
            dynamicColor = stored[KeyDynamicColor] ?: AppPreferences().dynamicColor,
        )
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KeyTheme] = theme.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KeyDynamicColor] = enabled }
    }

    private fun String.toAppTheme(): AppTheme? = AppTheme.entries.firstOrNull { it.name == this }

    private companion object {
        val KeyTheme = stringPreferencesKey("theme")
        val KeyDynamicColor = booleanPreferencesKey("dynamic_color")
    }
}
