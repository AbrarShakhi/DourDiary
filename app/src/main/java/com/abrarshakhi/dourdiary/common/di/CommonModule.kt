package com.abrarshakhi.dourdiary.common.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.abrarshakhi.dourdiary.common.data.power.AndroidBackgroundRestrictionChecker
import com.abrarshakhi.dourdiary.common.data.preferences.AppPreferencesDataStore
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.common.main.MainAppViewModel
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private const val PreferencesFileName = "app_preferences"

val commonModule = module {
    single<Clock> { Clock.systemDefaultZone() }

    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single<CoroutineScope>(DataStoreScope) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = get(DataStoreScope),
            produceFile = { androidContext().preferencesDataStoreFile(PreferencesFileName) },
        )
    }

    single<AppPreferencesRepository> { AppPreferencesDataStore(dataStore = get()) }

    single<BackgroundRestrictionChecker> { AndroidBackgroundRestrictionChecker(androidContext()) }

    viewModelOf(::MainAppViewModel)
}
