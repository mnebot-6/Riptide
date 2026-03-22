package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlin.random.Random

class LootboxResolver(
    private val marineCreatureRepository: MarineCreatureRepository
) {
    /**
     * Resuelve una lootbox: elige una especie aleatoria (ponderada por rareza)
     * entre las aún no desbloqueadas de la categoría.
     * Se llama cuando el usuario ABRE la lootbox, no cuando se gana.
     */
    suspend fun resolve(lootbox: PendingLootbox): CreatureSpec {
        val allSpecs = allCreatures.filter { it.category == lootbox.category }
        val unlockedSpecies = marineCreatureRepository.getByCategory(lootbox.category)
            .map { it.species }
            .toSet()
        val candidates = allSpecs.filter { it.species !in unlockedSpecies }

        if (candidates.isEmpty()) {
            // Fallback: no debería ocurrir, pero si todas están desbloqueadas devolvemos la primera
            return allSpecs.first()
        }

        return weightedRandom(candidates)
    }

    private fun weightedRandom(candidates: List<CreatureSpec>): CreatureSpec {
        val totalWeight = candidates.sumOf { it.rarity.weight.toDouble() }
        var roll = Random.nextDouble() * totalWeight
        for (spec in candidates) {
            roll -= spec.rarity.weight
            if (roll <= 0.0) return spec
        }
        return candidates.last()
    }
}
