package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NightSummaryProcessorTest {

    private val today = LocalDate(2024, 6, 15)

    // ── Task helpers ──────────────────────────────────────────────────────────

    private fun completedTask(id: String, blockId: String? = "blockA") = DayTask(
        id = id, blockId = blockId, title = "Task $id",
        schedule = TaskSchedule.OneTime(today, null),
        status = TaskStatus.COMPLETED,
        completedAt = null, postponedTo = null, sourceTaskId = null
    )

    private fun pendingTask(
        id: String,
        blockId: String? = "blockA",
        time: LocalTime? = LocalTime(12, 0)
    ) = DayTask(
        id = id, blockId = blockId, title = "Task $id",
        schedule = TaskSchedule.OneTime(today, time),
        status = TaskStatus.PENDING,
        completedAt = null, postponedTo = null, sourceTaskId = null
    )

    private fun postponedTask(id: String, blockId: String? = "blockA") = DayTask(
        id = id, blockId = blockId, title = "Task $id",
        schedule = TaskSchedule.OneTime(today, null),
        status = TaskStatus.POSTPONED,
        completedAt = null, postponedTo = null, sourceTaskId = null
    )

    private fun makeProcessor(
        taskRepo: FakeDayTaskRepository,
        summaryRepo: FakeDaySummaryRepository
    ) = NightSummaryProcessor(
        dayTaskRepository = taskRepo,
        daySummaryRepository = summaryRepo,
        blockStreakProcessor = null,
        ecosystemProcessor = null,
        userPreferencesRepository = null
    )

    // ── Already processed: no-op ──────────────────────────────────────────────

    @Test
    fun alreadyProcessed_noInsert() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
        }
        val summaryRepo = FakeDaySummaryRepository().also {
            it.insert(com.mnebot.riptide.domain.model.DaySummary(
                id = "existing", date = today, score = 1f,
                tasksTotal = 1, tasksCompleted = 1, streakDay = 0,
                feedbackMessage = "Already done"
            ))
        }
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        assertEquals(1, summaryRepo.inserted().size, "Should not insert a second summary")
    }

    // ── Empty tasks: no-op ────────────────────────────────────────────────────

    @Test
    fun emptyTasks_noSummaryInserted() = runTest {
        val taskRepo = FakeDayTaskRepository() // empty
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        assertTrue(summaryRepo.inserted().isEmpty())
    }

    // ── All completed: score = 1.0 ────────────────────────────────────────────

    @Test
    fun allCompleted_score1_summaryInserted() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(completedTask("t2"))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(today, summary.date)
        assertEquals(2, summary.tasksTotal)
        assertEquals(2, summary.tasksCompleted)
        assertEquals(1.0f, summary.score)
    }

    // ── Partial completion: correct score ─────────────────────────────────────

    @Test
    fun partialCompletion_correctScore() = runTest {
        val summaryTime = LocalTime(23, 0)
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))           // evaluable (completed)
            it.addTask(completedTask("t2"))           // evaluable (completed)
            it.addTask(pendingTask("t3", time = LocalTime(10, 0)))  // evaluable (time < summaryTime)
            it.addTask(pendingTask("t4", time = LocalTime(10, 0)))  // evaluable (time < summaryTime)
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today, summaryTime = summaryTime)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(4, summary.tasksTotal)
        assertEquals(2, summary.tasksCompleted)
        assertEquals(0.5f, summary.score)
    }

    // ── Postponed tasks excluded from evaluation ──────────────────────────────

    @Test
    fun postponedTasks_excluded() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(postponedTask("t2"))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        // Only t1 is evaluable (t2 is POSTPONED, excluded from nonPostponed)
        assertEquals(1, summary.tasksTotal)
        assertEquals(1, summary.tasksCompleted)
        assertEquals(1.0f, summary.score)
    }

    // ── No evaluable tasks → no summary ──────────────────────────────────────

    @Test
    fun noEvaluableTasks_noSummary() = runTest {
        // PENDING OneTime tasks with no time are NOT evaluable
        // (filter requires taskTime != null AND summaryTime != null)
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(pendingTask("t1", time = null))
            it.addTask(pendingTask("t2", time = null))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        // No summaryTime passed → pending tasks without time are not evaluable
        processor.processDay(today, summaryTime = null)

        assertTrue(summaryRepo.inserted().isEmpty(),
            "Should not create summary when no tasks are evaluable")
    }

    // ── Pending evaluable tasks are expired ───────────────────────────────────

    @Test
    fun evaluablePendingTasks_getExpired() = runTest {
        val summaryTime = LocalTime(23, 0)
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(pendingTask("t2", time = LocalTime(10, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today, summaryTime = summaryTime)

        // The pending task should have been expired
        val tasks = taskRepo.getByDate(today)
        val t2 = tasks.find { it.id == "t2" }
        assertEquals(TaskStatus.EXPIRED, t2?.status,
            "Evaluable PENDING tasks should be set to EXPIRED")
    }

    // ── Task with no blockId still counted ───────────────────────────────────

    @Test
    fun taskWithNullBlockId_isCountedInSummary() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", blockId = null))
            it.addTask(completedTask("t2", blockId = null))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(2, summary.tasksTotal)
        assertEquals(2, summary.tasksCompleted)
    }

    // ── Score 0 when all evaluable are pending (and expired) ─────────────────

    @Test
    fun allPendingEvaluable_score0() = runTest {
        val summaryTime = LocalTime(23, 0)
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(pendingTask("t1", time = LocalTime(9, 0)))
            it.addTask(pendingTask("t2", time = LocalTime(11, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today, summaryTime = summaryTime)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(2, summary.tasksTotal)
        assertEquals(0, summary.tasksCompleted)
        assertEquals(0f, summary.score)
    }
}
