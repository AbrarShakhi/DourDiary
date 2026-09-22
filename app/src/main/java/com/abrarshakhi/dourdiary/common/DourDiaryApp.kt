package com.abrarshakhi.dourdiary.common

import android.app.Application
import com.abrarshakhi.dourdiary.BuildConfig
import com.abrarshakhi.dourdiary.common.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DourDiaryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@DourDiaryApp)
            modules(appModules)
        }
    }
}
