package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.presentation.aquarium.CATEGORY_UNLOCK_LEVELS
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock

class EcosystemProcessor(
    private val ecosystemStateRepository: EcosystemStateRepository,
    private val marineCreatureRepository: MarineCreatureRepository
) {
    suspend fun addXpForTask(categories: List<MarineCategory>): List<PendingLootbox> {
        val targets = if (categories.isEmpty()) {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()
        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / targets.size
        return targets.flatMap { addXp(it, xpEach) }
    }

    suspend fun addNightBonus(
        score: Float,
        bestStreak: Int,
        categories: List<MarineCategory>
    ): List<PendingLootbox> {
        val targets = if (categories.isEmpty()) {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()
        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return emptyList()
        val xpEach = bonus / targets.size
        return targets.flatMap { addXp(it, xpEach) }
    }

    private suspend fun addXp(
        category: MarineCategory,
        xp: Int,
        fromOverflow: Boolean = false
    ): List<PendingLootbox> {
        if (xp <= 0) return emptyList()

        // ── XP overflow: si todas las especies de la categoría están desbloqueadas,
        //    50% del XP va a la categoría con menor nivel (catch-up) ──
        val allSpecsInCategory = allCreatures.filter { it.category == category }
        val unlockedCreatures = marineCreatureRepository.getByCategory(category)
        val allUnlocked = unlockedCreatures.size >= allSpecsInCategory.size

        val actualXp: Int
        var overflowLootboxes: List<PendingLootbox> = emptyList()

        if (allUnlocked && !fromOverflow) {
            actualXp = xp / 2
            val overflow = xp - actualXp
            if (overflow > 0) {
                overflowLootboxes = redistributeOverflow(category, overflow)
            }
        } else {
            actualXp = xp
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        val oldLevel = if (existing == null) 1 else existing.currentLevel
        val newXp = (existing?.totalExperience ?: 0) + actualXp
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

        // Repartir XP entre criaturas desbloqueadas de esta categoría
        val creatures = marineCreatureRepository.getByCategory(category)
        if (creatures.isNotEmpty()) {
            val xpPerCreature = actualXp / creatures.size
            if (xpPerCreature > 0) {
                creatures.forEach { creature ->
                    val newCreatureXp = creature.experience + xpPerCreature
                    val newCreatureLevel = EcosystemLevelCalculator.levelForXp(newCreatureXp)
                    marineCreatureRepository.update(
                        creature.copy(
                            experience = newCreatureXp,
                            creatureLevel = newCreatureLevel
                        )
                    )
                }
            }
        }

        // ── Detección de lootbox (reemplaza unlock detection fija) ──
        val unlockLevels = CATEGORY_UNLOCK_LEVELS[category] ?: emptyList()
        val triggeredLevels = unlockLevels.filter { it in (oldLevel + 1)..newLevel }

        // Solo generar lootboxes si hay especies por desbloquear
        val unlockedCount = marineCreatureRepository.getByCategory(category).size
        val totalSpecies = allSpecsInCategory.size
        val availableSlots = (totalSpecies - unlockedCount).coerceAtLeast(0)
        val lootboxes = triggeredLevels.take(availableSlots).map { level ->
            PendingLootbox(category = category, categoryLevel = level)
        }

        return lootboxes + overflowLootboxes
    }

    /**
     * Redistribuye XP overflow a la categoría con menor nivel (catch-up).
     * Si hay empate, elige aleatoriamente entre las empatadas.
     * Excluye DECORATION y categorías con todas las especies desbloqueadas.
     */
    private suspend fun redistributeOverflow(
        sourceCategory: MarineCategory,
        overflowXp: Int
    ): List<PendingLootbox> {
        val allStates = ecosystemStateRepository.getUnlocked()
        val candidates = allStates
            .filter { it.category != MarineCategory.DECORATION }
            .filter { it.category != sourceCategory }
            .filter { state ->
                val specsInCat = allCreatures.count { it.category == state.category }
                val unlockedInCat = marineCreatureRepository.getByCategory(state.category).size
                unlockedInCat < specsInCat // aún tiene especies por desbloquear
            }

        if (candidates.isEmpty()) return emptyList()

        val minLevel = candidates.minOf { it.currentLevel }
        val tied = candidates.filter { it.currentLevel == minLevel }
        val target = tied[Random.nextInt(tied.size)]

        return addXp(target.category, overflowXp, fromOverflow = true)
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
