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
    suspend fun processDay(date: LocalDate, blockIds: List<String>) {
        val yesterday = date.minus(1, DateTimeUnit.DAY)

        for (blockId in blockIds) {
            val tasksToday = dayTaskRepository.getByDateAndBlock(date, blockId)

            // Día neutral: sin tareas → no toca la racha
            if (tasksToday.isEmpty()) continue

            val hadActivity = tasksToday.any { it.status == TaskStatus.COMPLETED }
            val existing = blockStreakRepository.getByBlockId(blockId)

            if (hadActivity) {
                val isConsecutive = existing?.lastActiveDate == yesterday
                val newStreak = if (isConsecutive) (existing!!.currentStreak + 1) else 1

                if (existing == null) {
                    blockStreakRepository.insert(
                        BlockStreak(
                            blockId = blockId,
                            currentStreak = newStreak,
                            lastActiveDate = date
                        )
                    )
                } else {
                    blockStreakRepository.update(
                        existing.copy(currentStreak = newStreak, lastActiveDate = date)
                    )
                }

            } else {
                if (existing != null && existing.lastActiveDate != date) {
                    blockStreakRepository.update(existing.copy(currentStreak = 0))
                }
            }
        }
    }
}