package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mnebot.riptide.data.local.entity.EcosystemStateEntity

@Dao
interface EcosystemStateDao {
    @Query("SELECT * FROM ecosystem_states WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): EcosystemStateEntity?

    @Query("SELECT * FROM ecosystem_states")
    suspend fun getAll(): List<EcosystemStateEntity>

    @Query("SELECT * FROM ecosystem_states WHERE isUnlocked = 1")
    suspend fun getUnlocked(): List<EcosystemStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: EcosystemStateEntity)

    @Update
    suspend fun update(state: EcosystemStateEntity)
}