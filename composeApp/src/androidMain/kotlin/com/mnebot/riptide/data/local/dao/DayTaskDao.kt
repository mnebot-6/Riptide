package com.mnebot.riptide.data.local.dao

import androidx.room.*
import com.mnebot.riptide.data.local.entity.DayTaskEntity

@Dao
interface DayTaskDao {
    @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY time ASC, title ASC")
    suspend fun getByDate(date: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE blockId = :blockId")
    suspend fun getByBlock(blockId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE sourceTaskId = :sourceTaskId")
    suspend fun getBySourceTask(sourceTaskId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE status = 'PENDING' AND date < :date")
    suspend fun getPendingBefore(date: String): List<DayTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DayTaskEntity)

    @Update
    suspend fun update(task: DayTaskEntity)

    @Query("UPDATE day_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM day_tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM day_tasks WHERE sourceTaskId = :sourceTaskId")
    suspend fun deleteBySourceTask(sourceTaskId: String)

    @Query("DELETE FROM day_tasks WHERE sourceTaskId = :sourceTaskId AND (status = 'PENDING' OR status = 'POSTPONED') AND date >= :fromDate")
    suspend fun deleteBySourceTaskFromDate(sourceTaskId: String, fromDate: String)

    @Query("SELECT * FROM day_tasks WHERE date = :date AND blockId = :blockId")
    suspend fun getByDateAndBlock(date: String, blockId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE notificationsEnabled = 1 AND status = 'PENDING' AND time IS NOT NULL")
    suspend fun getPendingWithNotifications(): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE date >= :from AND date <= :to AND status IN ('COMPLETED','EXPIRED') ORDER BY date DESC, time ASC")
    suspend fun getCompletedRange(from: String, to: String): List<DayTaskEntity>
}