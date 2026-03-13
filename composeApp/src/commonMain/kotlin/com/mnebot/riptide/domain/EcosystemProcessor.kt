package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class EcosystemProcessor(
    private val ecosystemStateRepository: EcosystemStateRepository
) {
    suspend fun addXpForTask(categories: List<MarineCategory>): List<CreatureSpec> {
        if (categories.isEmpty()) return emptyList()
        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / categories.size
        val unlocked = mutableListOf<CreatureSpec>()
        categories.forEach { category ->
            unlocked += addXp(category, xpEach)
        }
        return unlocked
    }

    suspend fun addNightBonus(
        score: Float,
        bestStreak: Int,
        categories: List<MarineCategory>
    ): List<CreatureSpec> {
        if (categories.isEmpty()) return emptyList()
        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return emptyList()
        val xpEach = bonus / categories.size
        val unlocked = mutableListOf<CreatureSpec>()
        categories.forEach { category ->
            unlocked += addXp(category, xpEach)
        }
        return unlocked
    }

    private suspend fun addXp(category: MarineCategory, xp: Int): List<CreatureSpec> {
        if (xp <= 0) return emptyList()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        val oldLevel = existing?.currentLevel ?: 0
        val newXp = (existing?.totalExperience ?: 0) + xp
        val newLevel = EcosystemLevelCalculator.levelForXp(newXp)

        if (existing == null) {
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
            ecosystemStateRepository.update(
                existing.copy(
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    lastUpdated = now
                )
            )
        }

        // Detecta criaturas recién desbloqueadas (nivel cruzado entre oldLevel y newLevel)
        return allCreatures.filter { spec ->
            spec.category == category &&
                    spec.unlockLevel in (oldLevel + 1)..newLevel
        }
    }
}