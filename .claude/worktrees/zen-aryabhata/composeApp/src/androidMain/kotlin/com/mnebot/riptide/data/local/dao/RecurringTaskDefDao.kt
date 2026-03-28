package com.mnebot.riptide.data.local.dao

import androidx.room.*
import com.mnebot.riptide.data.local.entity.RecurringTaskDefEntity

@Dao
interface RecurringTaskDefDao {
    @Query("SELECT * FROM recurring_task_defs")
    suspend fun getAll(): List<RecurringTaskDefEntity>

    @Query("SELECT * FROM recurring_task_defs WHERE id = :id")
    suspend fun getById(id: String): RecurringTaskDefEntity?

    @Query("SELECT * FROM recurring_task_defs WHERE blockId = :blockId")
    suspend fun getByBlock(blockId: String): List<RecurringTaskDefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(def: RecurringTaskDefEntity)

    @Update
    suspend fun update(def: RecurringTaskDefEntity)

    @Query("DELETE FROM recurring_task_defs WHERE id = :id")
    suspend fun delete(id: String)
}