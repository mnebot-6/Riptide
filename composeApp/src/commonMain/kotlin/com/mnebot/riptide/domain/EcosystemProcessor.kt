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
                .filter { it != MarineCategory.DECORATION && it != MarineCategory.COMPANION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()

        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / targets.size
        val spilloverTarget = findSpilloverTarget(excludes = targets)

        return targets.flatMap { target ->
            if (spilloverTarget != null) {
                val actualXp    = (xpEach * 0.80).toInt()
                val spilloverXp = xpEach - actualXp
                val main  = addXp(target, actualXp)
                val spill = addXp(spilloverTarget, spilloverXp, fromSpillover = true)
                main + spill
            } else {
                addXp(target, xpEach)
            }
        }.distinctBy { it.category to it.categoryLevel }
    }

    suspend fun addNightBonus(
        score: Float,
        bestStreak: Int,
        categories: List<MarineCategory>
    ): List<PendingLootbox> {
        val targets = if (categories.isEmpty()) {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION && it != MarineCategory.COMPANION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()

        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return emptyList()

        val xpEach = bonus / targets.size
        val spilloverTarget = findSpilloverTarget(excludes = targets)

        return targets.flatMap { target ->
            if (spilloverTarget != null) {
                val actualXp    = (xpEach * 0.80).toInt()
                val spilloverXp = xpEach - actualXp
                val main  = addXp(target, actualXp)
                val spill = addXp(spilloverTarget, spilloverXp, fromSpillover = true)
                main + spill
            } else {
                addXp(target, xpEach)
            }
        }.distinctBy { it.category to it.categoryLevel }
    }

    /**
     * Returns the unlocked non-DECORATION category with least [EcosystemState.totalExperience],
     * excluding any category already in [excludes].
     * Returns null when no valid candidate exists.
     * On tie, picks randomly among tied candidates.
     */
    private suspend fun findSpilloverTarget(excludes: List<MarineCategory>): MarineCategory? {
        val candidates = ecosystemStateRepository.getUnlocked()
            .filter { it.category != MarineCategory.DECORATION && it.category != MarineCategory.COMPANION }
            .filter { it.category !in excludes }
        if (candidates.isEmpty()) return null
        val minXp = candidates.minOf { it.totalExperience }
        val tied  = candidates.filter { it.totalExperience == minXp }
        return tied[Random.nextInt(tied.size)].category
    }

    private suspend fun addXp(
        category: MarineCategory,
        xp: Int,
        fromOverflow: Boolean = false,
        fromSpillover: Boolean = false
    ): List<PendingLootbox> {
        if (xp <= 0) return emptyList()

        // ── XP overflow: si todas las especies de la categoría están desbloqueadas,
        //    50% del XP va a la categoría con menor nivel (catch-up) ──
        // Neither overflow nor spillover propagate further when already redistributed
        val allSpecsInCategory = allCreatures.filter { it.category == category }
        val unlockedCreatures  = marineCreatureRepository.getByCategory(category)
        val allUnlocked        = unlockedCreatures.size >= allSpecsInCategory.size
        val isRedistributed    = fromOverflow || fromSpillover

        val actualXp: Int
        var overflowLootboxes: List<PendingLootbox> = emptyList()

        if (allUnlocked && !isRedistributed) {
            actualXp = xp / 2
            val overflow = xp - actualXp
            if (overflow > 0) {
                overflowLootboxes = redistributeOverflow(category, overflow)
            }
        } else {
            actualXp = xp
        }

        val now      = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        val oldLevel = if (existing == null) 1 else existing.currentLevel
        val newXp    = (existing?.totalExperience ?: 0) + actualXp
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
                    val newCreatureXp    = creature.experience + xpPerCreature
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

        // ── Detección de lootbox ──
        val unlockLevels    = CATEGORY_UNLOCK_LEVELS[category] ?: emptyList()
        val triggeredLevels = unlockLevels.filter { it in (oldLevel + 1)..newLevel }

        val unlockedCount  = marineCreatureRepository.getByCategory(category).size
        val totalSpecies   = allSpecsInCategory.size
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
            .filter { it.category != MarineCategory.DECORATION && it.category != MarineCategory.COMPANION }
            .filter { it.category != sourceCategory }
            .filter { state ->
                val specsInCat    = allCreatures.count { it.category == state.category }
                val unlockedInCat = marineCreatureRepository.getByCategory(state.category).size
                unlockedInCat < specsInCat
            }
        if (candidates.isEmpty()) return emptyList()
        val minLevel = candidates.minOf { it.currentLevel }
        val tied     = candidates.filter { it.currentLevel == minLevel }
        val target   = tied[Random.nextInt(tied.size)]
        return addXp(target.category, overflowXp, fromOverflow = true)
    }

    suspend fun unlockCategory(category: MarineCategory) {
        val now      = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
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
