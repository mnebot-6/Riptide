package com.mnebot.riptide.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mnebot.riptide.data.local.dao.*
import com.mnebot.riptide.data.local.entity.*

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // DayTask: contable, notas, timer, prioridad
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN targetCount INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN currentCount INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN notes TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN timerDurationMinutes INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN isPriority INTEGER NOT NULL DEFAULT 0")
        // RecurringTaskDef: contable, notas plantilla, timer, prioridad
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN targetCount INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN noteTemplate TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN timerDurationMinutes INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN isPriority INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE work_blocks ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE block_categories ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE day_summaries ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE block_streaks ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE ecosystem_states ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE marine_creatures ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_blocks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE day_tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE recurring_task_defs ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE block_streaks ADD COLUMN longestStreak INTEGER NOT NULL DEFAULT 0"
        )
    }
}

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
    version = 13
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