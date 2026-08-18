package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.fakes.FakeUserPreferencesRepository
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecorationUnlockCheckerTest {

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

    private var taskCounter = 0
    private fun makeTask(status: TaskStatus) = DayTask(
        id = "task-${taskCounter++}",
        blockId = "block1",
        title = "Task",
        schedule = TaskSchedule.OneTime(today, null),
        status = status,
        completedAt = null,
        sourceTaskId = null
    )

    private fun makeCreature(species: CreatureSpecies) = MarineCreature(
        id = "creature-${species.name}",
        ecosystemId = MarineCategory.DECORATION.name,
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

    // ── TREASURE_CHEST ────────────────────────────────────────────────────────

    @Test
    fun checkTreasureChest_fewerThan7Summaries_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        for (i in 0..5) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkTreasureChest())
    }

    @Test
    fun checkTreasureChest_oneDayBelowThreshold_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        // 0.5 queda por debajo del umbral de día conseguido (0.80) y rompe la racha
        summaryRepo.insert(makeSummary(today, 0.5f))
        for (i in 1..6) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkTreasureChest())
    }

    @Test
    fun checkTreasureChest_dateGapInStreak_returnsNull() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        // Days 26, 25, 24, 23, 21, 20, 19 — gap between 23rd and 21st
        val days = listOf(26, 25, 24, 23, 21, 20, 19)
        days.forEach { day ->
            summaryRepo.insert(makeSummary(LocalDate(2026, 3, day), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo)
        assertNull(checker.checkTreasureChest())
    }

    @Test
    fun checkTreasureChest_7ConsecutivePerfectDays_unlocksCreature() = runTest {
        val summaryRepo  = FakeDaySummaryRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        for (i in 0..6) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo, creatureRepo = creatureRepo)

        val result = checker.checkTreasureChest()
        assertEquals(CreatureSpecies.TREASURE_CHEST, result)
        assertTrue(
            creatureRepo.all().any { it.species == CreatureSpecies.TREASURE_CHEST },
            "TREASURE_CHEST creature should be inserted"
        )
    }

    @Test
    fun checkTreasureChest_alreadyUnlocked_returnsNull() = runTest {
        val summaryRepo  = FakeDaySummaryRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        creatureRepo.insert(makeCreature(CreatureSpecies.TREASURE_CHEST))
        for (i in 0..6) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        val checker = buildChecker(summaryRepo = summaryRepo, creatureRepo = creatureRepo)
        assertNull(checker.checkTreasureChest())
    }

    // ── ANCHOR ────────────────────────────────────────────────────────────────

    @Test
    fun checkAnchor_fewerThan100Tasks_returnsNull() = runTest {
        val taskRepo = FakeDayTaskRepository()
        repeat(99) { taskRepo.addTask(makeTask(TaskStatus.COMPLETED)) }
        val checker = buildChecker(taskRepo = taskRepo)
        assertNull(checker.checkAnchor())
    }

    @Test
    fun checkAnchor_exactly100CompletedTasks_unlocksCreature() = runTest {
        val taskRepo     = FakeDayTaskRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        repeat(100) { taskRepo.addTask(makeTask(TaskStatus.COMPLETED)) }
        val checker = buildChecker(taskRepo = taskRepo, creatureRepo = creatureRepo)

        val result = checker.checkAnchor()
        assertEquals(CreatureSpecies.ANCHOR, result)
        assertTrue(
            creatureRepo.all().any { it.species == CreatureSpecies.ANCHOR },
            "ANCHOR creature should be inserted"
        )
    }

    @Test
    fun checkAnchor_alreadyUnlocked_returnsNull() = runTest {
        val taskRepo     = FakeDayTaskRepository()
        val creatureRepo = FakeMarineCreatureRepository()
        repeat(100) { taskRepo.addTask(makeTask(TaskStatus.COMPLETED)) }
        creatureRepo.insert(makeCreature(CreatureSpecies.ANCHOR))
        val checker = buildChecker(taskRepo = taskRepo, creatureRepo = creatureRepo)
        assertNull(checker.checkAnchor())
    }

    // ── SUNKEN_SHIP ───────────────────────────────────────────────────────────

    @Test
    fun checkSunkenShip_wallpaperNotActivated_returnsNull() = runTest {
        val checker = buildChecker()
        assertNull(checker.checkSunkenShip())
    }

    @Test
    fun checkSunkenShip_wallpaperActivated_unlocksCreature() = runTest {
        val creatureRepo = FakeMarineCreatureRepository()
        val prefsRepo    = FakeUserPreferencesRepository()
        prefsRepo.activateWallpaper()
        val checker = buildChecker(creatureRepo = creatureRepo, prefsRepo = prefsRepo)

        val result = checker.checkSunkenShip()
        assertEquals(CreatureSpecies.SUNKEN_SHIP, result)
        assertTrue(
            creatureRepo.all().any { it.species == CreatureSpecies.SUNKEN_SHIP },
            "SUNKEN_SHIP creature should be inserted"
        )
    }

    // ── getProgress ───────────────────────────────────────────────────────────

    @Test
    fun getProgress_returnsCorrectCounts() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        val taskRepo    = FakeDayTaskRepository()
        val prefsRepo   = FakeUserPreferencesRepository()
        prefsRepo.activateWallpaper()

        // 3 consecutive perfect days
        for (i in 0..2) {
            summaryRepo.insert(makeSummary(today - DatePeriod(days = i), 1.0f))
        }
        // 42 completed tasks
        repeat(42) { taskRepo.addTask(makeTask(TaskStatus.COMPLETED)) }

        val checker  = buildChecker(summaryRepo = summaryRepo, taskRepo = taskRepo, prefsRepo = prefsRepo)
        val progress = checker.getProgress()

        assertEquals(3, progress.perfectDaysStreak)
        assertEquals(42, progress.completedTasksTotal)
        assertTrue(progress.wallpaperActivated)
    }
}
