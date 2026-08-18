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
import kotlin.time.Clock

/**
 * Reparto de XP, en dos reglas y sin recursión:
 *
 * 1. El 80% se reparte entre las categorías marinas del bloque.
 * 2. El 20% restante va a la categoría desbloqueada con menos XP (catch-up).
 *
 * La sorpresa vive en LootboxResolver —qué especie sale—, no en la contabilidad:
 * si el XP no cuadra, es un bug, no el azar.
 */
class EcosystemProcessor(
    private val ecosystemStateRepository: EcosystemStateRepository,
    private val marineCreatureRepository: MarineCreatureRepository
) {
    suspend fun addXpForTask(categories: List<MarineCategory>): List<PendingLootbox> =
        distribute(EcosystemLevelCalculator.XP_PER_TASK, categories)

    suspend fun addNightBonus(
        score: Float,
        streak: Int,
        fullBlocks: Int,
        categories: List<MarineCategory>
    ): List<PendingLootbox> =
        distribute(EcosystemLevelCalculator.nightBonus(score, streak, fullBlocks), categories)

    /** Reparte [xp] siguiendo las dos reglas de la clase. */
    private suspend fun distribute(
        xp: Int,
        categories: List<MarineCategory>
    ): List<PendingLootbox> {
        if (xp <= 0) return emptyList()

        val targets = categories.ifEmpty { unlockedTargets() }
        if (targets.isEmpty()) return emptyList()

        val spilloverXp = (xp * SPILLOVER_FRACTION).toInt()
        val mainXp = xp - spilloverXp

        val lootboxes = mutableListOf<PendingLootbox>()

        val xpEach = mainXp / targets.size
        targets.forEach { target -> lootboxes += addXp(target, xpEach) }

        // Catch-up: el resto va a la categoría más rezagada. Si resulta ser una de las
        // del bloque, se lo queda igual — sigue siendo la que menos tiene.
        leastAdvancedCategory()?.let { lootboxes += addXp(it, spilloverXp) }

        return lootboxes.distinctBy { it.category to it.categoryLevel }
    }

    private suspend fun unlockedTargets(): List<MarineCategory> =
        ecosystemStateRepository.getUnlocked()
            .map { it.category }
            .filter { it !in NON_XP_CATEGORIES }

    /** Categoría desbloqueada con menos XP total. Determinista: desempata por nombre. */
    private suspend fun leastAdvancedCategory(): MarineCategory? =
        ecosystemStateRepository.getUnlocked()
            .filter { it.category !in NON_XP_CATEGORIES }
            .minWithOrNull(compareBy({ it.totalExperience }, { it.category.name }))
            ?.category

    private suspend fun addXp(category: MarineCategory, xp: Int): List<PendingLootbox> {
        if (xp <= 0) return emptyList()

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        val oldLevel = existing?.currentLevel ?: 1
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

        // El nivel visual de cada criatura se deriva del de su categoría
        // (MarineCreature.visualLevel): no hay un segundo pozo de XP que mantener.

        val unlockLevels = CATEGORY_UNLOCK_LEVELS[category] ?: emptyList()
        val triggeredLevels = unlockLevels.filter { it in (oldLevel + 1)..newLevel }
        if (triggeredLevels.isEmpty()) return emptyList()

        val totalSpecies = allCreatures.count { it.category == category }
        val unlockedCount = marineCreatureRepository.getByCategory(category).size
        val availableSlots = (totalSpecies - unlockedCount).coerceAtLeast(0)

        return triggeredLevels.take(availableSlots).map { level ->
            PendingLootbox(category = category, categoryLevel = level)
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

    private companion object {
        /** Parte del XP que va a la categoría más rezagada. */
        const val SPILLOVER_FRACTION = 0.20

        /** Categorías que no participan en el reparto de XP. */
        val NON_XP_CATEGORIES = setOf(MarineCategory.DECORATION, MarineCategory.COMPANION)
    }
}
