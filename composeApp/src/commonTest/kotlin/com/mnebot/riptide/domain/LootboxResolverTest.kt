package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LootboxResolverTest {

    private fun resolver(repo: FakeMarineCreatureRepository = FakeMarineCreatureRepository()) =
        LootboxResolver(repo)

    private fun creature(species: CreatureSpecies, ecosystemId: String = "eco1") = MarineCreature(
        id = "c-${species.name}",
        ecosystemId = ecosystemId,
        species = species,
        nickname = null,
        unlockedAtLevel = 1,
        experience = 0,
        creatureLevel = 1,
        unlockedAt = LocalDateTime(2024, 6, 15, 12, 0)
    )

    // ── directSpecies ────────────────────────────────────────────────────────

    @Test
    fun directSpecies_returns_exact_species() = runTest {
        val lootbox = PendingLootbox(
            category = MarineCategory.DECORATION,
            categoryLevel = 1,
            directSpecies = CreatureSpecies.TREASURE_CHEST
        )
        val result = resolver().resolve(lootbox)
        assertEquals(CreatureSpecies.TREASURE_CHEST, result.species)
    }

    @Test
    fun directSpecies_ignores_already_unlocked() = runTest {
        val repo = FakeMarineCreatureRepository()
        repo.insert(creature(CreatureSpecies.TREASURE_CHEST))

        val lootbox = PendingLootbox(
            category = MarineCategory.DECORATION,
            categoryLevel = 1,
            directSpecies = CreatureSpecies.TREASURE_CHEST
        )
        // directSpecies siempre devuelve la especie indicada, incluso si ya esta desbloqueada
        val result = resolver(repo).resolve(lootbox)
        assertEquals(CreatureSpecies.TREASURE_CHEST, result.species)
    }

    // ── weighted random ──────────────────────────────────────────────────────

    @Test
    fun random_returns_species_from_correct_category() = runTest {
        val lootbox = PendingLootbox(
            category = MarineCategory.FISH,
            categoryLevel = 2
        )
        val result = resolver().resolve(lootbox)
        assertEquals(MarineCategory.FISH, result.category)
    }

    @Test
    fun random_excludes_already_unlocked_species() = runTest {
        val repo = FakeMarineCreatureRepository()
        // Desbloquear todas las FISH excepto SUNFISH
        val fishSpecies = CreatureSpecies.entries.filter { it.category == MarineCategory.FISH }
        fishSpecies.filter { it != CreatureSpecies.SUNFISH }.forEach {
            repo.insert(creature(it))
        }

        val lootbox = PendingLootbox(category = MarineCategory.FISH, categoryLevel = 5)
        val result = resolver(repo).resolve(lootbox)
        assertEquals(CreatureSpecies.SUNFISH, result.species)
    }

    @Test
    fun random_single_candidate_always_returns_it() = runTest {
        val repo = FakeMarineCreatureRepository()
        // Desbloquear todas las CRUSTACEAN excepto BARNACLE
        val crustaceans = CreatureSpecies.entries.filter { it.category == MarineCategory.CRUSTACEAN }
        crustaceans.filter { it != CreatureSpecies.BARNACLE }.forEach {
            repo.insert(creature(it))
        }

        val lootbox = PendingLootbox(category = MarineCategory.CRUSTACEAN, categoryLevel = 4)
        // Con un solo candidato, siempre devuelve ese
        repeat(10) {
            val result = resolver(repo).resolve(lootbox)
            assertEquals(CreatureSpecies.BARNACLE, result.species)
        }
    }

    @Test
    fun fallback_when_all_unlocked_returns_first_spec() = runTest {
        val repo = FakeMarineCreatureRepository()
        // Desbloquear todas las FLORA
        CreatureSpecies.entries.filter { it.category == MarineCategory.FLORA }.forEach {
            repo.insert(creature(it))
        }

        val lootbox = PendingLootbox(category = MarineCategory.FLORA, categoryLevel = 10)
        val result = resolver(repo).resolve(lootbox)
        // Fallback: devuelve la primera spec de la categoria
        val firstFloraSpec = allCreatures.first { it.category == MarineCategory.FLORA }
        assertEquals(firstFloraSpec.species, result.species)
    }

    @Test
    fun random_result_has_valid_rarity() = runTest {
        val lootbox = PendingLootbox(category = MarineCategory.MOLLUSK, categoryLevel = 2)
        val result = resolver().resolve(lootbox)
        assertNotNull(result.rarity)
        assertTrue(result.rarity.weight > 0f)
    }

    @Test
    fun random_distribution_respects_weights() = runTest {
        // Ejecutar muchas resoluciones y verificar que COMMON aparece mas que LEGENDARY
        val repo = FakeMarineCreatureRepository()
        val lootbox = PendingLootbox(category = MarineCategory.FISH, categoryLevel = 2)

        val results = mutableMapOf<CreatureRarity, Int>()
        repeat(500) {
            val result = resolver(repo).resolve(lootbox)
            results[result.rarity] = (results[result.rarity] ?: 0) + 1
        }

        val commonCount = results[CreatureRarity.COMMON] ?: 0
        val legendaryCount = results[CreatureRarity.LEGENDARY] ?: 0
        // COMMON (0.40) deberia aparecer mucho mas que LEGENDARY (0.02)
        assertTrue(commonCount > legendaryCount, "COMMON ($commonCount) should appear more than LEGENDARY ($legendaryCount)")
    }
}
