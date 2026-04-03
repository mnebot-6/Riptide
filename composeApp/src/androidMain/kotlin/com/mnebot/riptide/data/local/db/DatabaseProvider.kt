package com.mnebot.riptide.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    private var instance: RiptideDatabase? = null

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE day_tasks ADD COLUMN hasBeenRewarded INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun getDatabase(context: Context): RiptideDatabase {
        return instance ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                RiptideDatabase::class.java,
                "riptide.db"
            )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build().also { instance = it }
        }
    }
}