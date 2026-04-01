package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.BlockStreakDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.repository.BlockStreakRepository

class BlockStreakRepositoryImpl(private val dao: BlockStreakDao) : BlockStreakRepository {
    override suspend fun getByBlockId(blockId: String): BlockStreak? =
        dao.getByBlockId(blockId)?.toDomain()

    override suspend fun getAll(): List<BlockStreak> =
        dao.getAll().map { it.toDomain() }

    override suspend fun insert(streak: BlockStreak) =
        dao.insert(streak.toEntity().copy(updatedAt = nowIso()))

    override suspend fun update(streak: BlockStreak) =
        dao.update(streak.toEntity().copy(updatedAt = nowIso()))
}