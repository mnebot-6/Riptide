package com.mnebot.riptide.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mnebot.riptide.data.local.dao.BlockCategoryDao
import com.mnebot.riptide.data.local.dao.BlockStreakDao
import com.mnebot.riptide.data.local.dao.DaySummaryDao
import com.mnebot.riptide.data.local.dao.DayTaskDao
import com.mnebot.riptide.data.local.dao.EcosystemStateDao
import com.mnebot.riptide.data.local.dao.MarineCreatureDao
import com.mnebot.riptide.data.local.dao.WorkBlockDao
import com.mnebot.riptide.data.local.entity.BlockStreakEntity
import com.mnebot.riptide.data.local.entity.DaySummaryEntity
import com.mnebot.riptide.data.local.entity.DayTaskEntity
import com.mnebot.riptide.data.local.entity.EcosystemStateEntity
import com.mnebot.riptide.data.local.entity.MarineCreatureEntity
import com.mnebot.riptide.data.local.entity.WorkBlockEntity
import com.mnebot.riptide.data.local.entity.BlockCategoryEntity


@Database(
    entities = [
        WorkBlockEntity::class,
        DayTaskEntity::class,
        DaySummaryEntity::class,
        BlockStreakEntity::class,
        EcosystemStateEntity::class,
        MarineCreatureEntity::class,
        BlockCategoryEntity::class
    ],
    version = 5
)
abstract class RiptideDatabase : RoomDatabase() {
    abstract fun workBlockDao(): WorkBlockDao
    abstract fun dayTaskDao(): DayTaskDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun blockStreakDao(): BlockStreakDao
    abstract fun ecosystemStateDao(): EcosystemStateDao
    abstract fun marineCreatureDao(): MarineCreatureDao
    abstract fun blockCategoryDao(): BlockCategoryDao
}