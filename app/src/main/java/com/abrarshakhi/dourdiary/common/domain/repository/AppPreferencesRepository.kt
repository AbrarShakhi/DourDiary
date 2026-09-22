package com.abrarshakhi.dourdiary.common.domain.repository

import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {

    val preferences: Flow<AppPreferences>

    suspend fun setTheme(theme: AppTheme)

    suspend fun setDynamicColor(enabled: Boolean)
}
