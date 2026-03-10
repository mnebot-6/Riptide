package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mnebot.riptide.data.local.entity.DaySummaryEntity

@Dao
interface DaySummaryDao {
    @Query("SELECT * FROM day_summaries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DaySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DaySummaryEntity)
}