package com.abrarshakhi.dourdiary.common.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abrarshakhi.dourdiary.common.data.database.dao.RunDao
import com.abrarshakhi.dourdiary.common.data.database.dao.RunPointDao
import com.abrarshakhi.dourdiary.common.data.database.entity.RunEntity
import com.abrarshakhi.dourdiary.common.data.database.entity.RunPointEntity

@Database(
    entities = [RunEntity::class, RunPointEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class DourDiaryDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao

    abstract fun runPointDao(): RunPointDao

    companion object {
        const val Name = "dour_diary.db"

        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE runs ADD COLUMN simplifiedRoute TEXT")
            }
        }

        val Migrations = arrayOf(Migration1To2)
    }
}
