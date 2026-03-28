package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcosystemProcessorSpilloverTest {

    private val now = LocalDateTime(2026, 3, 26, 12, 0)

    private fun makeState(
        category: MarineCategory,
        totalXp: Int,
        isUnlocked: Boolean = true
    ) = EcosystemState(
        id = category.name,
        category = category,
        totalExperience = totalXp,
        currentLevel = EcosystemLevelCalculator.levelForXp(totalXp),
        isUnlocked = isUnlocked,
        lastUpdated = now
    )

    // ── No spillover when only one unlocked non-DECORATION category ───────────

    @Test
    fun addXpForTask_singleCategory_noSpillover_fullXpToTarget() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        stateRepo.seed(makeState(MarineCategory.FISH, totalXp = 0))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishState = stateRepo.all().first { it.category == MarineCategory.FISH }
        assertEquals(EcosystemLevelCalculator.XP_PER_TASK, fishState.totalExperience)
    }

    // ── Spillover: 80% to target, 20% to weakest unlocked other category ─────

    @Test
    fun addXpForTask_twoCategories_spilloverGoesToWeakest() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        // FISH is the target; FLORA is unlocked with 0 XP (weakest)
        stateRepo.seed(makeState(MarineCategory.FISH,  totalXp = 50))
        stateRepo.seed(makeState(MarineCategory.FLORA, totalXp = 0))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val xp        = EcosystemLevelCalculator.XP_PER_TASK   // 10
        val expected80 = (xp * 0.80).toInt()                   // 8
        val expected20 = xp - expected80                        // 2

        val fishState  = stateRepo.all().first { it.category == MarineCategory.FISH }
        val floraState = stateRepo.all().first { it.category == MarineCategory.FLORA }

        assertEquals(50 + expected80, fishState.totalExperience,
            "FISH should receive 80% of XP")
        assertEquals(0 + expected20, floraState.totalExperience,
            "FLORA (weakest) should receive 20% spillover")
    }

    @Test
    fun addXpForTask_totalDistributedEqualsXpPerTask() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        stateRepo.seed(makeState(MarineCategory.FISH,  totalXp = 100))
        stateRepo.seed(makeState(MarineCategory.FLORA, totalXp = 0))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishXp  = stateRepo.all().first { it.category == MarineCategory.FISH  }.totalExperience - 100
        val floraXp = stateRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience - 0

        assertEquals(
            EcosystemLevelCalculator.XP_PER_TASK,
            fishXp + floraXp,
            "80% + 20% must equal 100% of XP_PER_TASK"
        )
    }

    // ── Spillover does not cascade (fromSpillover flag) ───────────────────────

    @Test
    fun addXpForTask_spilloverRecipientDoesNotTriggerFurtherSpillover() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        // Three unlocked categories; FLORA is the weakest and will receive spillover
        stateRepo.seed(makeState(MarineCategory.FISH,        totalXp = 50))
        stateRepo.seed(makeState(MarineCategory.FLORA,       totalXp = 0))
        stateRepo.seed(makeState(MarineCategory.CRUSTACEAN,  totalXp = 30))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        processor.addXpForTask(listOf(MarineCategory.FISH))

        // CRUSTACEAN should NOT have received any XP (it's not the spillover target)
        val crustXp = stateRepo.all().first { it.category == MarineCategory.CRUSTACEAN }.totalExperience
        assertEquals(30, crustXp, "CRUSTACEAN should not receive cascaded spillover XP")
    }

    // ── DECORATION is excluded from spillover candidates ─────────────────────

    @Test
    fun addXpForTask_decorationExcludedFromSpillover() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        // FISH is target; DECORATION is unlocked but must not receive spillover
        stateRepo.seed(makeState(MarineCategory.FISH,       totalXp = 50))
        stateRepo.seed(makeState(MarineCategory.DECORATION, totalXp = 0))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        // No other non-DECORATION candidate → no spillover → FISH gets full XP
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishXp  = stateRepo.all().first { it.category == MarineCategory.FISH       }.totalExperience
        val decoXp  = stateRepo.all().first { it.category == MarineCategory.DECORATION }.totalExperience

        assertEquals(50 + EcosystemLevelCalculator.XP_PER_TASK, fishXp,
            "With no valid spillover target FISH receives full XP")
        assertEquals(0, decoXp,
            "DECORATION must not receive spillover XP")
    }

    // ── Weakest unlocked category is chosen as spillover target ──────────────

    @Test
    fun addXpForTask_weakestCategoryReceivesSpillover() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        stateRepo.seed(makeState(MarineCategory.FISH,       totalXp = 50))
        stateRepo.seed(makeState(MarineCategory.FLORA,      totalXp = 5))   // weakest
        stateRepo.seed(makeState(MarineCategory.CRUSTACEAN, totalXp = 30))

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val floraXp = stateRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        val crustXp = stateRepo.all().first { it.category == MarineCategory.CRUSTACEAN }.totalExperience

        val xp = EcosystemLevelCalculator.XP_PER_TASK
        assertTrue(floraXp > 5, "FLORA (weakest) should have received spillover XP")
        assertEquals(30, crustXp, "CRUSTACEAN (stronger) should NOT receive spillover XP")
    }

    // ── Locked categories are excluded from spillover ────────────────────────

    @Test
    fun addXpForTask_lockedCategoryExcludedFromSpillover() = runTest {
        val stateRepo    = FakeEcosystemStateRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        stateRepo.seed(makeState(MarineCategory.FISH,  totalXp = 50, isUnlocked = true))
        stateRepo.seed(makeState(MarineCategory.FLORA, totalXp = 0,  isUnlocked = false)) // locked

        val processor = EcosystemProcessor(stateRepo, creatureRepo)
        // No unlocked non-DECORATION candidate other than target → no spillover
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val floraXp = stateRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        assertEquals(0, floraXp, "Locked category must not receive spillover XP")

        val fishXp = stateRepo.all().first { it.category == MarineCategory.FISH }.totalExperience
        assertEquals(50 + EcosystemLevelCalculator.XP_PER_TASK, fishXp,
            "FISH receives full XP when no valid spillover target")
    }
}
