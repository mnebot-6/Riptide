package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.BlockStreakRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Días de racha consecutiva que otorgan una lootbox bonus. */
val STREAK_MILESTONES = listOf(7, 14, 30)

/** Resultado por bloque tras procesar el día. */
data class BlockStreakUpdate(
    val currentStreak: Int,
    /** Hitos (de STREAK_MILESTONES) que se cruzaron exactamente hoy. */
    val milestonesReached: List<Int> = emptyList()
)

class BlockStreakProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val blockStreakRepository: BlockStreakRepository
) {
    suspend fun processDay(date: LocalDate, blockIds: List<String>): Map<String, BlockStreakUpdate> {
        val yesterday = date.minus(1, DateTimeUnit.DAY)
        val result = mutableMapOf<String, BlockStreakUpdate>()

        for (blockId in blockIds) {
            val tasksToday = dayTaskRepository.getByDateAndBlock(date, blockId)
            if (tasksToday.isEmpty()) continue  // día neutral

            val allCompleted = tasksToday.all { it.status == TaskStatus.COMPLETED }
            val existing = blockStreakRepository.getByBlockId(blockId)

            if (allCompleted) {
                val oldStreak = existing?.currentStreak ?: 0
                val isConsecutive = existing?.lastActiveDate == yesterday
                val newStreak = if (isConsecutive) oldStreak + 1 else 1
                val newLongest = maxOf(existing?.longestStreak ?: 0, newStreak)

                if (existing == null) {
                    blockStreakRepository.insert(BlockStreak(blockId, newStreak, date, longestStreak = newLongest))
                } else {
                    blockStreakRepository.update(existing.copy(currentStreak = newStreak, lastActiveDate = date, longestStreak = newLongest))
                }

                // Detectar hitos cruzados: oldStreak < hito <= newStreak
                val milestones = STREAK_MILESTONES.filter { it in (oldStreak + 1)..newStreak }

                result[blockId] = BlockStreakUpdate(newStreak, milestones)
            } else {
                if (existing != null && existing.lastActiveDate != date) {
                    blockStreakRepository.update(existing.copy(currentStreak = 0))
                }
                result[blockId] = BlockStreakUpdate(0)
            }
        }

        return result
    }
}
