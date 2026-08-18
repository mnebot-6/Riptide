package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.fakes.FakeUserPreferencesRepository
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the 3 new decoration unlock conditions added in Sprint Sync:
 * DIVING_HELMET (Google sign-in), CORAL_THRONE (14 perfect days), GOLDEN_TRIDENT (complete a category).
 */
class DecorationUnlockNewDecorationsTest {

    private val now   = LocalDateTime(2026, 3, 26, 12, 0)
    private val today = LocalDate(2026, 3, 26)

    private fun makeSummary(date: LocalDate, score: Float) = DaySummary(
        id = date.toString(),
        date = date,
        score = score,
        tasksTotal = 5,
        tasksCompleted = if (score == 1.0f) 5 else 3,
        feedbackMessage = ""
    )

    private fun makeCreature(species: CreatureSpecies, cat: MarineCategory = MarineCategory.DECORATION) =
        MarineCreature(
            id = "creature-${species.name}",
            ecosystemId = cat.name,
            species = species,
            nickname = null,
            unlockedAtLevel = 1,
            experience = 0,
            creatureLevel = 1,
            unlockedAt = now
        )

    private fun buildChecker(
        summaryRepo: FakeDaySummaryRepository = FakeDaySummaryRepository(),
        taskRepo: FakeDayTaskRepository = FakeDayTaskRepository(),
        creatureRepo: FakeMarineCreatureRepository = FakeMarineCreatureRepository(),
        prefsRepo: FakeUserPreferencesRepository = FakeUserPreferencesRepository()
    ): DecorationUnlockChecker {
        val ecosystemRepo = FakeEcosystemStateRepository()
        val processor     = EcosystemProcessor(ecosystemRepo, creatureRepo)
        return DecorationUnlockChecker(
            daySummaryRepository      = summaryRepo,
            dayTaskRepository         = taskRepo,
            marineCreatureRepository  = creatureRepo,
            ecosystemProcessor        = processor,
            userPreferencesRepository = prefsRepo
        )
    }

    // ── DIVING_HELMET ─────────────────────────────────────────────────────────

    @Test
    fun checkDivingHelmet_notSignedIn_returnsNull() = runTest {
        val checker = buildChecker()
        assertNull(checker.checkDivingHelmet())
    }

    @Test
    fun checkDivingHelmet_signedIn_unlocksCreature() = runTest {
        val prefsRepo    = FakeUserPreferencesRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        prefsRepo.saveUser(LoggedInUser("u1", "test@test.com", "Test", null))

        val checker = buildChecker(prefsRepo = prefsRepo, creatureRepo = creatureRepo)
        val result = checker.checkDivingHelmet()

        assertEquals(CreatureSpecies.DIVING_HELMET, result)
        assertTrue(
            creatureRepo.all().any { it.species == CreatureSpecies.DIVING_HELMET },
            "DIVING_HELMET creature should be inserted"
        )
    }

    @Test
    fun checkDivingHelmet_alreadyUnlocked_returnsNull() = runTest {
        val prefsRepo    = FakeUserPreferencesRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        prefsRepo.saveUser(LoggedInUser("u1", "test@test.com", "Test", null))
        creatureRepo.insert(makeCreature(CreatureSpecies.DIVING_HELMET))

        val checker = buildChecker(prefsRepo = prefsRepo, creatureRepo = creatureRepo)
        assertNull(checker.checkDivingHelmet())
    }

    // ── CORAL_THRONE ──────────────────────────────────────────────────────────

    @Test
    fun checkCoralThrone_fewerThan14Days_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        for (i in 0..12) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkCoralThrone())
    }

    @Test
    fun checkCoralThrone_14DaysButOneImperfect_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        for (i in 0..13) {
            val score = if (i == 7) 0.5f else 1.0f
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), score))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkCoralThrone())
    }

    @Test
    fun checkCoralThrone_14ConsecutivePerfectDays_unlocksCreature() = runTest {
        val summaryRepo  = FakeDaySummaryRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        for (i in 0..13) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }

        val checker = buildChecker(summaryRepo = summaryRepo, creatureRepo = creatureRepo)
        val result = checker.checkCoralThrone()

        assertEquals(CreatureSpecies.CORAL_THRONE, result)
        assertTrue(
            creatureRepo.all().any { it.species == CreatureSpecies.CORAL_THRONE },
            "CORAL_THRONE creature should be inserted"
        )
    }

    @Test
    fun checkCoralThrone_alreadyUnlocked_returnsNull() = runTest {
        val summaryRepo  = FakeDaySummaryRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        creatureRepo.insert(makeCreature(CreatureSpecies.CORAL_THRONE))
        for (i in 0..13) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo, creatureRepo = creatureRepo)
        assertNull(checker.checkCoralThrone())
    }

    @Test
    fun checkCoralThrone_dateGapBreaksStreak_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        // 14 perfect days but with a gap between day 6 and day 8
        val days = listOf(0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14)
        days.forEach { d ->
            summaryRepo.insert(makeSummary(today - DatePeriod(days = d), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkCoralThrone())
    }

    // ── GOLDEN_TRIDENT ────────────────────────────────────────────────────────

    @Test
    fun checkGoldenTrident_noCategoryComplete_returnsNull() = runTest {
        val checker = buildChecker()
        assertNull(checker.checkGoldenTrident())
    }

    @Test
    fun checkGoldenTrident_oneCategoryComplete_unlocksCreature() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        // Unlock ALL species in CEPHALOPOD (smallest lootbox category typically)
        val cephalopodSpecies = allCreatures.filter { it.category == MarineCategory.CEPHALOPOD }
        cephalopodSpecies.forEach { spec ->
            creatureRepo.insert(makeCreature(spec.species, MarineCategory.CEPHALOPOD))
        }

        val checker = buildChecker(creatureRepo = creatureRepo)
        val result = checker.checkGoldenTrident()

        assertEquals(CreatureSpecies.GOLDEN_TRIDENT, result)
    }

    @Test
    fun checkGoldenTrident_alreadyUnlocked_returnsNull() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        creatureRepo.insert(makeCreature(CreatureSpecies.GOLDEN_TRIDENT))
        // Complete a category too
        val cephalopodSpecies = allCreatures.filter { it.category == MarineCategory.CEPHALOPOD }
        cephalopodSpecies.forEach { spec ->
            creatureRepo.insert(makeCreature(spec.species, MarineCategory.CEPHALOPOD))
        }
        val checker = buildChecker(creatureRepo = creatureRepo)
        assertNull(checker.checkGoldenTrident())
    }

    // ── checkAll ──────────────────────────────────────────────────────────────

    @Test
    fun checkAll_multipleConditionsMet_returnsAllUnlocked() = runTest {
        val summaryRepo  = FakeDaySummaryRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        val prefsRepo    = FakeUserPreferencesRepository()
        val taskRepo     = FakeDayTaskRepository()

        // Meet DIVING_HELMET condition
        prefsRepo.saveUser(LoggedInUser("u1", "test@test.com", "Test", null))

        // Meet TREASURE_CHEST condition (7 consecutive perfect days)
        for (i in 0..6) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }

        val checker = buildChecker(
            summaryRepo = summaryRepo,
            taskRepo = taskRepo,
            creatureRepo = creatureRepo,
            prefsRepo = prefsRepo
        )
        val results = checker.checkAll()

        assertTrue(results.contains(CreatureSpecies.TREASURE_CHEST), "Should unlock TREASURE_CHEST")
        assertTrue(results.contains(CreatureSpecies.DIVING_HELMET), "Should unlock DIVING_HELMET")
    }

    // ── getProgress — new fields ──────────────────────────────────────────────

    @Test
    fun getProgress_includesGoogleSignIn() = runTest {
        val prefsRepo = FakeUserPreferencesRepository()
        prefsRepo.saveUser(LoggedInUser("u1", "test@test.com", "Test User", null))

        val checker  = buildChecker(prefsRepo = prefsRepo)
        val progress = checker.getProgress()

        assertTrue(progress.googleSignedIn, "googleSignedIn should be true when user is logged in")
    }

    @Test
    fun getProgress_notSignedIn_googleSignedInFalse() = runTest {
        val checker  = buildChecker()
        val progress = checker.getProgress()

        assertTrue(!progress.googleSignedIn, "googleSignedIn should be false when not logged in")
    }

    @Test
    fun getProgress_longestPerfectStreak_reflects14Days() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        for (i in 0..13) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker  = buildChecker(summaryRepo = summaryRepo)
        val progress = checker.getProgress()

        assertEquals(14, progress.longestPerfectStreak)
    }
}
