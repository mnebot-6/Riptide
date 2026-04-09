package com.mnebot.riptide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mnebot.riptide.data.local.entity.PersonalDateEntity

@Dao
interface PersonalDateDao {
    @Query("SELECT * FROM personal_dates ORDER BY date ASC")
    suspend fun getAll(): List<PersonalDateEntity>

    @Query("SELECT * FROM personal_dates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PersonalDateEntity?

    @Query("SELECT * FROM personal_dates WHERE date >= :from AND date <= :to ORDER BY date ASC")
    suspend fun getByDateRange(from: String, to: String): List<PersonalDateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PersonalDateEntity)

    @Query("DELETE FROM personal_dates WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM personal_dates WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: String): List<PersonalDateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PersonalDateEntity>)

    @Query("UPDATE personal_dates SET updatedAt = :now WHERE updatedAt = ''")
    suspend fun stampUpdatedAt(now: String)
}
