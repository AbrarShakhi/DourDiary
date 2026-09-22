package com.abrarshakhi.dourdiary.common.di

import androidx.room.Room
import com.abrarshakhi.dourdiary.common.data.database.DourDiaryDatabase
import com.abrarshakhi.dourdiary.common.data.database.dao.RunDao
import com.abrarshakhi.dourdiary.common.data.database.dao.RunPointDao
import com.abrarshakhi.dourdiary.common.data.repository.RoomRunRepository
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = DourDiaryDatabase::class.java,
            name = DourDiaryDatabase.Name,
        )
            .addMigrations(*DourDiaryDatabase.Migrations)
            .build()
    }

    single<RunDao> { get<DourDiaryDatabase>().runDao() }
    single<RunPointDao> { get<DourDiaryDatabase>().runPointDao() }

    single<RunRepository> {
        RoomRunRepository(
            runDao = get(),
            runPointDao = get(),
            ioDispatcher = get(IoDispatcher),
        )
    }
}
