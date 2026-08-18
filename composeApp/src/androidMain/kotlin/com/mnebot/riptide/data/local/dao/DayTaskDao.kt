package com.mnebot.riptide.data.local.dao

import androidx.room.*
import com.mnebot.riptide.data.local.entity.DayTaskEntity

@Dao
interface DayTaskDao {
    @Query("SELECT * FROM day_tasks WHERE id = :id")
    suspend fun getById(id: String): DayTaskEntity?

    @Query("SELECT * FROM day_tasks WHERE date = :date AND isDeleted = 0 ORDER BY time ASC, title ASC")
    suspend fun getByDate(date: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE date >= :from AND date <= :to AND isDeleted = 0 ORDER BY time ASC, title ASC")
    suspend fun getByDateRange(from: String, to: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE blockId = :blockId AND isDeleted = 0")
    suspend fun getByBlock(blockId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE sourceTaskId = :sourceTaskId AND isDeleted = 0")
    suspend fun getBySourceTask(sourceTaskId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE status = 'PENDING' AND date < :date AND isDeleted = 0")
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

    @Query("DELETE FROM day_tasks WHERE sourceTaskId = :sourceTaskId AND status = 'PENDING' AND date >= :fromDate")
    suspend fun deleteBySourceTaskFromDate(sourceTaskId: String, fromDate: String)

    /** Incluye las borradas a propósito: el generador no debe recrearlas. */
    @Query("SELECT DISTINCT sourceTaskId FROM day_tasks WHERE date = :date AND sourceTaskId IS NOT NULL")
    suspend fun getSourceIdsForDate(date: String): List<String>

    @Query("SELECT * FROM day_tasks WHERE date = :date AND blockId = :blockId AND isDeleted = 0")
    suspend fun getByDateAndBlock(date: String, blockId: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE notificationsEnabled = 1 AND status = 'PENDING' AND time IS NOT NULL AND isDeleted = 0")
    suspend fun getPendingWithNotifications(): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE date >= :from AND date <= :to AND status IN ('COMPLETED','EXPIRED') AND isDeleted = 0 ORDER BY date DESC, time ASC")
    suspend fun getCompletedRange(from: String, to: String): List<DayTaskEntity>

    @Query("SELECT COUNT(*) FROM day_tasks WHERE status = 'COMPLETED' AND isDeleted = 0")
    suspend fun countCompleted(): Int

    @Query("SELECT * FROM day_tasks WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<DayTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DayTaskEntity>)

    @Query("UPDATE day_tasks SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)

    @Query("UPDATE day_tasks SET isDeleted = 1, updatedAt = :now WHERE sourceTaskId = :sourceTaskId")
    suspend fun softDeleteBySourceTask(sourceTaskId: String, now: String)

    @Query("UPDATE day_tasks SET isDeleted = 1, updatedAt = :now WHERE sourceTaskId = :sourceTaskId AND date >= :fromDate")
    suspend fun softDeleteBySourceTaskFromDate(sourceTaskId: String, fromDate: String, now: String)
}