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
        val targets = categories.ifEmpty {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        }
        if (targets.isEmpty()) return emptyList()
        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / targets.size
        return targets.flatMap { addXp(it, xpEach) }
    }

    suspend fun addNightBonus(
        score: Float,
        bestStreak: Int,
        categories: List<MarineCategory>
    ): List<CreatureSpec> {
        val targets = categories.ifEmpty {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        }
        if (targets.isEmpty()) return emptyList()
        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return emptyList()
        val xpEach = bonus / targets.size
        return targets.flatMap { addXp(it, xpEach) }
    }

    private suspend fun addXp(category: MarineCategory, xp: Int): List<CreatureSpec> {
        if (xp <= 0) return emptyList()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        // Si no existe aún, el nivel de partida para detectar desbloqueos es 1 (nivel base)
        val oldLevel = if (existing == null) 1 else existing.currentLevel
        val newXp = (existing?.totalExperience ?: 0) + xp
        val newLevel = EcosystemLevelCalculator.levelForXp(newXp)

        if (existing == null) {
            ecosystemStateRepository.insert(
                EcosystemState(
                    id = generateUUID(),
                    category = category,
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    isUnlocked = category.isUnlockedByDefault,
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

        return allCreatures.filter { spec ->
            spec.category == category &&
                    spec.unlockLevel in (oldLevel + 1)..newLevel
        }
    }

    suspend fun unlockCategory(category: MarineCategory) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)
        if (existing == null) {
            ecosystemStateRepository.insert(
                EcosystemState(
                    id = generateUUID(),
                    category = category,
                    totalExperience = 0,
                    currentLevel = 1,
                    isUnlocked = true,
                    lastUpdated = now
                )
            )
        } else {
            ecosystemStateRepository.update(existing.copy(isUnlocked = true))
        }
    }
}