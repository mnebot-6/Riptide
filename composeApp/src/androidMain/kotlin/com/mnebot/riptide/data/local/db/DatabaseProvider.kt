package com.mnebot.riptide.data.local.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: RiptideDatabase? = null

    fun getDatabase(context: Context): RiptideDatabase {
        return instance ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                RiptideDatabase::class.java,
                "riptide.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .build().also { instance = it }
        }
    }
}

private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE work_blocks ADD COLUMN icon TEXT NOT NULL DEFAULT 'work'")
    }
}