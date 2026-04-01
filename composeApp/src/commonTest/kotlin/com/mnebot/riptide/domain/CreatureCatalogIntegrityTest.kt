package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.presentation.aquarium.CATEGORY_UNLOCK_LEVELS
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that verify the integrity of the creature catalog, unlock levels,
 * and species enum. These catch mismatches when adding new species.
 */
class CreatureCatalogIntegrityTest {

    // ── Every species enum has a CreatureSpec ─────────────────────────────────

    @Test
    fun allSpeciesEnums_haveCatalogEntry() {
        val catalogSpecies = allCreatures.map { it.species }.toSet()
        val enumSpecies = CreatureSpecies.entries.toSet()
        val missing = enumSpecies - catalogSpecies
        assertTrue(
            missing.isEmpty(),
            "Species enum values missing from allCreatures catalog: $missing"
        )
    }

    @Test
    fun noDuplicateSpeciesInCatalog() {
        val species = allCreatures.map { it.species }
        val duplicates = species.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(
            duplicates.isEmpty(),
            "Duplicate species in allCreatures: $duplicates"
        )
    }

    // ── Category counts match ────────────────────────────────────────────────

    @Test
    fun speciesEnum_categoryCounts_matchCatalog() {
        val lootboxCategories = listOf(
            MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC, MarineCategory.CEPHALOPOD,
            MarineCategory.REPTILE, MarineCategory.MAMMAL
        )
        for (cat in lootboxCategories) {
            val enumCount = CreatureSpecies.entries.count { it.category == cat }
            val catalogCount = allCreatures.count { it.category == cat }
            assertEquals(
                enumCount, catalogCount,
                "Mismatch for $cat: enum has $enumCount but catalog has $catalogCount"
            )
        }
    }

    // ── Unlock levels sufficient for all species ─────────────────────────────

    @Test
    fun unlockLevels_haveEnoughForAllSpecies() {
        val lootboxCategories = listOf(
            MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC, MarineCategory.CEPHALOPOD,
            MarineCategory.REPTILE, MarineCategory.MAMMAL
        )
        for (cat in lootboxCategories) {
            val speciesCount = allCreatures.count { it.category == cat }
            val levelCount = CATEGORY_UNLOCK_LEVELS[cat]?.size ?: 0
            assertTrue(
                levelCount >= speciesCount,
                "$cat has $speciesCount species but only $levelCount unlock levels"
            )
        }
    }

    @Test
    fun unlockLevels_areSorted() {
        for ((cat, levels) in CATEGORY_UNLOCK_LEVELS) {
            val sorted = levels.sorted()
            assertEquals(
                sorted, levels,
                "Unlock levels for $cat are not sorted: $levels"
            )
        }
    }

    @Test
    fun unlockLevels_allPositive() {
        for ((cat, levels) in CATEGORY_UNLOCK_LEVELS) {
            assertTrue(
                levels.all { it > 0 },
                "Unlock levels for $cat contain non-positive values: $levels"
            )
        }
    }

    // ── Decoration category has no unlock levels ─────────────────────────────

    @Test
    fun decoration_hasNoUnlockLevels() {
        val decoLevels = CATEGORY_UNLOCK_LEVELS[MarineCategory.DECORATION]
        assertTrue(
            decoLevels == null || decoLevels.isEmpty(),
            "DECORATION should not have unlock levels (decorations use special conditions)"
        )
    }

    // ── Species enum categories match catalog categories ─────────────────────

    @Test
    fun speciesEnum_categoryMatchesCatalog() {
        for (spec in allCreatures) {
            assertEquals(
                spec.species.category, spec.category,
                "Species ${spec.species}: enum category is ${spec.species.category} " +
                    "but catalog category is ${spec.category}"
            )
        }
    }

    // ── All creatures have valid rarity ───────────────────────────────────────

    @Test
    fun allCreatures_haveRarity() {
        for (spec in allCreatures) {
            // Just verify rarity is set (not null) — the enum guarantees valid values
            assertTrue(
                spec.rarity.name.isNotEmpty(),
                "${spec.species} has empty rarity"
            )
        }
    }

    // ── At least one creature per lootbox category ───────────────────────────

    @Test
    fun everyLootboxCategory_hasAtLeastOneCreature() {
        val lootboxCategories = listOf(
            MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC, MarineCategory.CEPHALOPOD,
            MarineCategory.REPTILE, MarineCategory.MAMMAL
        )
        for (cat in lootboxCategories) {
            val count = allCreatures.count { it.category == cat }
            assertTrue(count > 0, "$cat has no creatures in catalog")
        }
    }
}
