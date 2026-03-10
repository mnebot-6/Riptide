package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mnebot.riptide.data.local.entity.DayTaskEntity

@Dao
interface DayTaskDao {
    @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY `order` ASC")
    suspend fun getByDate(date: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE blockId = :blockId ORDER BY date DESC")
    suspend fun getByBlock(blockId: String): List<DayTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DayTaskEntity)

    @Update
    suspend fun update(task: DayTaskEntity)

    @Query("DELETE FROM day_tasks WHERE id = :id")
    suspend fun delete(id: String)
}