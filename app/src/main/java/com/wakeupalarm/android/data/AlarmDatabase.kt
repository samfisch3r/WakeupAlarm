package com.wakeupalarm.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class, Settings::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN globalVolume INTEGER NOT NULL DEFAULT 100")
                    db.execSQL("ALTER TABLE settings ADD COLUMN globalVibrate INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE settings ADD COLUMN startVolume INTEGER NOT NULL DEFAULT 20")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN startVolume INTEGER NOT NULL DEFAULT 20")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                .addMigrations(MIGRATION_5_7, MIGRATION_6_7)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
