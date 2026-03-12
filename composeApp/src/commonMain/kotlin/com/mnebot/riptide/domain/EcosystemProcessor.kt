package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class EcosystemProcessor(
    private val ecosystemStateRepository: EcosystemStateRepository
) {
    suspend fun addXpForTask(categories: List<MarineCategory>) {
        if (categories.isEmpty()) return
        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / categories.size
        categories.forEach { category ->
            addXp(category, xpEach)
        }
    }

    suspend fun addNightBonus(score: Float, bestStreak: Int, categories: List<MarineCategory>) {
        if (categories.isEmpty()) return
        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return
        val xpEach = bonus / categories.size
        categories.forEach { category ->
            addXp(category, xpEach)
        }
    }

    private suspend fun addXp(category: MarineCategory, xp: Int) {
        if (xp <= 0) return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)
        if (existing == null) {
            val newXp = xp
            val newLevel = EcosystemLevelCalculator.levelForXp(newXp)
            ecosystemStateRepository.insert(
                EcosystemState(
                    id = generateUUID(),
                    category = category,
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    lastUpdated = now
                )
            )
        } else {
            val newXp = existing.totalExperience + xp
            val newLevel = EcosystemLevelCalculator.levelForXp(newXp)
            ecosystemStateRepository.update(
                existing.copy(
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    lastUpdated = now
                )
            )
        }
    }
}