package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mnebot.riptide.data.local.entity.WorkBlockEntity

@Dao
interface WorkBlockDao {
    @Query("SELECT * FROM work_blocks WHERE isDeleted = 0")
    suspend fun getAll(): List<WorkBlockEntity>

    @Query("SELECT * FROM work_blocks WHERE id = :id")
    suspend fun getById(id: String): WorkBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: WorkBlockEntity)

    @Update
    suspend fun update(block: WorkBlockEntity)

    @Query("DELETE FROM work_blocks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM work_blocks WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<WorkBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<WorkBlockEntity>)

    @Query("UPDATE work_blocks SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)
}