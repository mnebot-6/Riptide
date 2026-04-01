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

    @Query("SELECT * FROM day_summaries WHERE date >= :from AND date <= :to ORDER BY date DESC")
    suspend fun getRange(from: String, to: String): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summaries ORDER BY date DESC LIMIT :n")
    suspend fun getLatestN(n: Int): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summaries ORDER BY date DESC")
    suspend fun getAll(): List<DaySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DaySummaryEntity)

    @Query("SELECT * FROM day_summaries WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<DaySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DaySummaryEntity>)

    @Query("UPDATE day_summaries SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)
}