package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mnebot.riptide.data.local.entity.MarineCreatureEntity

@Dao
interface MarineCreatureDao {
    @Query("SELECT * FROM marine_creatures")
    suspend fun getAll(): List<MarineCreatureEntity>

    @Query("SELECT * FROM marine_creatures WHERE ecosystemId = :ecosystemId")
    suspend fun getByEcosystem(ecosystemId: String): List<MarineCreatureEntity>

    @Query("SELECT * FROM marine_creatures WHERE category = :category")
    suspend fun getByCategory(category: String): List<MarineCreatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creature: MarineCreatureEntity)

    @Update
    suspend fun update(creature: MarineCreatureEntity)
}