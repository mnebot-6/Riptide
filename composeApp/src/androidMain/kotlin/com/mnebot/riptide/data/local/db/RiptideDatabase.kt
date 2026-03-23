package com.mnebot.riptide.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mnebot.riptide.data.local.dao.*
import com.mnebot.riptide.data.local.entity.*

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE day_tasks ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE recurring_task_defs ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(
    entities = [
        WorkBlockEntity::class,
        BlockCategoryEntity::class,
        DayTaskEntity::class,
        RecurringTaskDefEntity::class,
        DaySummaryEntity::class,
        BlockStreakEntity::class,
        EcosystemStateEntity::class,
        MarineCreatureEntity::class
    ],
    version = 10
)

abstract class RiptideDatabase : RoomDatabase() {
    abstract fun workBlockDao(): WorkBlockDao
    abstract fun blockCategoryDao(): BlockCategoryDao
    abstract fun dayTaskDao(): DayTaskDao
    abstract fun recurringTaskDefDao(): RecurringTaskDefDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun blockStreakDao(): BlockStreakDao
    abstract fun ecosystemStateDao(): EcosystemStateDao
    abstract fun marineCreatureDao(): MarineCreatureDao
}