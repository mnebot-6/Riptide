package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.BlockStreak

interface BlockStreakRepository {
    suspend fun getByBlockId(blockId: String): BlockStreak?
    suspend fun getAll(): List<BlockStreak>
    suspend fun insert(streak: BlockStreak)
    suspend fun update(streak: BlockStreak)
}