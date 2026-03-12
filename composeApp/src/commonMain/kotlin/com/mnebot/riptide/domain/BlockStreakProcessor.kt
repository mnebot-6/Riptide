package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.BlockStreakRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class BlockStreakProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val blockStreakRepository: BlockStreakRepository
) {
    suspend fun processDay(date: LocalDate, blockIds: List<String>): Map<String, Int> {
        val yesterday = date.minus(1, DateTimeUnit.DAY)
        val result = mutableMapOf<String, Int>()

        for (blockId in blockIds) {
            val tasksToday = dayTaskRepository.getByDateAndBlock(date, blockId)
            if (tasksToday.isEmpty()) continue  // día neutral

            val hadActivity = tasksToday.any { it.status == TaskStatus.COMPLETED }
            val existing = blockStreakRepository.getByBlockId(blockId)

            if (hadActivity) {
                val isConsecutive = existing?.lastActiveDate == yesterday
                val newStreak = if (isConsecutive) (existing!!.currentStreak + 1) else 1
                if (existing == null) {
                    blockStreakRepository.insert(BlockStreak(blockId, newStreak, date))
                } else {
                    blockStreakRepository.update(existing.copy(currentStreak = newStreak, lastActiveDate = date))
                }
                result[blockId] = newStreak
            } else {
                if (existing != null && existing.lastActiveDate != date) {
                    blockStreakRepository.update(existing.copy(currentStreak = 0))
                }
                result[blockId] = 0
            }
        }

        return result
    }
}