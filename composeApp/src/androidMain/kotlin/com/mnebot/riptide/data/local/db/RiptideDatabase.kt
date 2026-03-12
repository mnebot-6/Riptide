package com.mnebot.riptide.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mnebot.riptide.data.local.dao.*
import com.mnebot.riptide.data.local.entity.*

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
    version = 6
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