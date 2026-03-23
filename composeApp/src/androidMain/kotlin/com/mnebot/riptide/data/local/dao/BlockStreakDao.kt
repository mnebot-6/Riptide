package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mnebot.riptide.data.local.entity.BlockStreakEntity

@Dao
interface BlockStreakDao {
    @Query("SELECT * FROM block_streaks WHERE blockId = :blockId")
    suspend fun getByBlockId(blockId: String): BlockStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: BlockStreakEntity)

    @Query("SELECT * FROM block_streaks")
    suspend fun getAll(): List<BlockStreakEntity>

    @Update
    suspend fun update(streak: BlockStreakEntity)
}