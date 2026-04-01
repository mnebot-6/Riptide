package com.mnebot.riptide.data.local.dao

import androidx.room.*
import com.mnebot.riptide.data.local.entity.RecurringTaskDefEntity

@Dao
interface RecurringTaskDefDao {
    @Query("SELECT * FROM recurring_task_defs WHERE isDeleted = 0")
    suspend fun getAll(): List<RecurringTaskDefEntity>

    @Query("SELECT * FROM recurring_task_defs WHERE id = :id")
    suspend fun getById(id: String): RecurringTaskDefEntity?

    @Query("SELECT * FROM recurring_task_defs WHERE blockId = :blockId AND isDeleted = 0")
    suspend fun getByBlock(blockId: String): List<RecurringTaskDefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(def: RecurringTaskDefEntity)

    @Update
    suspend fun update(def: RecurringTaskDefEntity)

    @Query("DELETE FROM recurring_task_defs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM recurring_task_defs WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<RecurringTaskDefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RecurringTaskDefEntity>)

    @Query("UPDATE recurring_task_defs SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)
}