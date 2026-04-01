package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mnebot.riptide.data.local.entity.BlockCategoryEntity

@Dao
interface BlockCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockCategoryEntity)

    @Query("SELECT * FROM block_categories WHERE blockId = :blockId")
    suspend fun getForBlock(blockId: String): List<BlockCategoryEntity>

    @Query("SELECT * FROM block_categories WHERE blockId IN (:blockIds)")
    suspend fun getForBlocks(blockIds: List<String>): List<BlockCategoryEntity>

    @Query("DELETE FROM block_categories WHERE blockId = :blockId")
    suspend fun deleteForBlock(blockId: String)

    @Query("DELETE FROM block_categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM block_categories WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<BlockCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BlockCategoryEntity>)

    @Query("UPDATE block_categories SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)
}