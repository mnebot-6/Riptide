package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.repository.BlockStreakRepository

class FakeBlockStreakRepository : BlockStreakRepository {

    private val streaks = mutableMapOf<String, BlockStreak>()

    fun getAll_snapshot(): Map<String, BlockStreak> = streaks.toMap()

    override suspend fun getByBlockId(blockId: String): BlockStreak? = streaks[blockId]
    override suspend fun getAll(): List<BlockStreak> = streaks.values.toList()
    override suspend fun insert(streak: BlockStreak) { streaks[streak.blockId] = streak }
    override suspend fun update(streak: BlockStreak) { streaks[streak.blockId] = streak }
}
