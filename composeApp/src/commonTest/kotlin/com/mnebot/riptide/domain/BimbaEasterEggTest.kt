package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.fakes.FakeUserPreferencesRepository
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the Bimba easter egg companion unlock.
 */
class BimbaEasterEggTest {

    private val now = LocalDateTime(2026, 3, 26, 12, 0)

    private fun buildChecker(
        creatureRepo: FakeMarineCreatureRepository = FakeMarineCreatureRepository(),
        prefsRepo: FakeUserPreferencesRepository = FakeUserPreferencesRepository()
    ): DecorationUnlockChecker {
        val ecosystemRepo = FakeEcosystemStateRepository()
        val processor     = EcosystemProcessor(ecosystemRepo, creatureRepo)
        return DecorationUnlockChecker(
            daySummaryRepository      = FakeDaySummaryRepository(),
            dayTaskRepository         = FakeDayTaskRepository(),
            marineCreatureRepository  = creatureRepo,
            ecosystemProcessor        = processor,
            userPreferencesRepository = prefsRepo
        )
    }

    // ── Trigger words ─────────────────────────────────────────────────────────

    @Test
    fun checkBimba_titleContainsPremio_unlocks() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        val checker = buildChecker(creatureRepo = creatureRepo)
        val result = checker.checkBimba("Dale un premio a Bimba")
        assertEquals(CreatureSpecies.BIMBA, result)
        assertTrue(creatureRepo.all().any { it.species == CreatureSpecies.BIMBA })
    }

    @Test
    fun checkBimba_titleContainsTreat_unlocks() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        val checker = buildChecker(creatureRepo = creatureRepo)
        val result = checker.checkBimba("Give the cat a treat")
        assertEquals(CreatureSpecies.BIMBA, result)
    }

    @Test
    fun checkBimba_caseInsensitive_unlocks() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        val checker = buildChecker(creatureRepo = creatureRepo)
        assertEquals(CreatureSpecies.BIMBA, checker.checkBimba("PREMIO para el gato"))
    }

    @Test
    fun checkBimba_noTriggerWord_returnsNull() = runTest {
        val checker = buildChecker()
        assertNull(checker.checkBimba("Do the dishes"))
    }

    @Test
    fun checkBimba_emptyTitle_returnsNull() = runTest {
        val checker = buildChecker()
        assertNull(checker.checkBimba(""))
    }

    @Test
    fun checkBimba_alreadyUnlocked_returnsNull() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        creatureRepo.insert(MarineCreature(
            id = "bimba-1",
            ecosystemId = MarineCategory.COMPANION.name,
            species = CreatureSpecies.BIMBA,
            nickname = "Bimba",
            unlockedAtLevel = 1,
            experience = 0,
            creatureLevel = 1,
            unlockedAt = now
        ))
        val checker = buildChecker(creatureRepo = creatureRepo)
        assertNull(checker.checkBimba("Give a treat"))
    }

    @Test
    fun checkBimba_queuesLootbox() = runTest {
        val prefsRepo = FakeUserPreferencesRepository()
        val checker = buildChecker(prefsRepo = prefsRepo)
        checker.checkBimba("premio")

        val lootboxes = prefsRepo.getPendingLootboxes()
        assertEquals(1, lootboxes.size)
        assertEquals(MarineCategory.COMPANION, lootboxes[0].category)
        assertEquals(CreatureSpecies.BIMBA, lootboxes[0].directSpecies)
    }

    @Test
    fun checkBimba_unlocksCompanionCategory() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        val ecosystemRepo = FakeEcosystemStateRepository()
        val processor = EcosystemProcessor(ecosystemRepo, creatureRepo)
        val checker = DecorationUnlockChecker(
            daySummaryRepository = FakeDaySummaryRepository(),
            dayTaskRepository = FakeDayTaskRepository(),
            marineCreatureRepository = creatureRepo,
            ecosystemProcessor = processor,
            userPreferencesRepository = FakeUserPreferencesRepository()
        )

        checker.checkBimba("premio")

        // COMPANION category should now be unlocked
        val companion = ecosystemRepo.all().find { it.category == MarineCategory.COMPANION }
        assertTrue(companion != null && companion.isUnlocked, "COMPANION category should be unlocked")
    }
}
