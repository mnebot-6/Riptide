package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * El cierre ocurre a las 23:59:59: TODA tarea del día cuenta, con hora o sin ella.
 * score = completadas / total, sin ponderación.
 */
class NightSummaryProcessorTest {

    private val today = LocalDate(2024, 6, 15)

    // ── Task helpers ──────────────────────────────────────────────────────────

    private fun completedTask(id: String, blockId: String? = "blockA") = DayTask(
        id = id, blockId = blockId, title = "Task $id",
        schedule = TaskSchedule.OneTime(today, null),
        status = TaskStatus.COMPLETED,
        completedAt = null, sourceTaskId = null
    )

    private fun pendingTask(
        id: String,
        blockId: String? = "blockA",
        time: LocalTime? = LocalTime(12, 0)
    ) = DayTask(
        id = id, blockId = blockId, title = "Task $id",
        schedule = TaskSchedule.OneTime(today, time),
        status = TaskStatus.PENDING,
        completedAt = null, sourceTaskId = null
    )

    private fun makeProcessor(
        taskRepo: FakeDayTaskRepository,
        summaryRepo: FakeDaySummaryRepository
    ) = NightSummaryProcessor(
        dayTaskRepository = taskRepo,
        daySummaryRepository = summaryRepo,
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
            it.insert(DaySummary(
                id = "existing", date = today, score = 1f,
                tasksTotal = 1, tasksCompleted = 1,                feedbackMessage = "Already done"
            ))
        }
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        assertEquals(1, summaryRepo.inserted().size, "Should not insert a second summary")
    }

    // ── Día neutral: se guarda igualmente con total 0 ─────────────────────────

    @Test
    fun emptyDay_insertsNeutralSummary() = runTest {
        val taskRepo = FakeDayTaskRepository() // empty
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary, "Un día sin tareas debe quedar registrado como neutral")
        assertEquals(0, summary.tasksTotal)
        assertEquals(0, summary.tasksCompleted)
        assertEquals(0f, summary.score)
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

    // ── Partial completion: score plano ───────────────────────────────────────

    @Test
    fun partialCompletion_correctScore() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(completedTask("t2"))
            it.addTask(pendingTask("t3", time = LocalTime(10, 0)))
            it.addTask(pendingTask("t4", time = LocalTime(10, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(4, summary.tasksTotal)
        assertEquals(2, summary.tasksCompleted)
        assertEquals(0.5f, summary.score)
    }

    // ── Tareas sin hora: cuentan igual que las demás ──────────────────────────

    @Test
    fun untimedTasks_areCountedAndExpired() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(pendingTask("t2", time = null))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(2, summary.tasksTotal, "Una tarea sin hora también cuenta")
        assertEquals(1, summary.tasksCompleted)
        assertEquals(0.5f, summary.score)

        val t2 = taskRepo.getByDate(today).find { it.id == "t2" }
        assertEquals(TaskStatus.EXPIRED, t2?.status, "Sin hora también expira al cerrar el día")
    }

    // ── Una tarea a las 23:00 se evalúa el mismo día ──────────────────────────

    @Test
    fun lateTask_isEvaluatedSameDay() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(pendingTask("t1", time = LocalTime(23, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(1, summary.tasksTotal)
        assertEquals(0f, summary.score)
    }

    // ── Pending tasks are expired ─────────────────────────────────────────────

    @Test
    fun pendingTasks_getExpired() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1"))
            it.addTask(pendingTask("t2", time = LocalTime(10, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val t2 = taskRepo.getByDate(today).find { it.id == "t2" }
        assertEquals(TaskStatus.EXPIRED, t2?.status, "PENDING pasa a EXPIRED al cerrar")
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

    // ── Score 0 cuando no se completó nada ───────────────────────────────────

    @Test
    fun allPending_score0() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(pendingTask("t1", time = LocalTime(9, 0)))
            it.addTask(pendingTask("t2", time = LocalTime(11, 0)))
        }
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(taskRepo, summaryRepo)

        processor.processDay(today)

        val summary = summaryRepo.inserted().firstOrNull()
        assertNotNull(summary)
        assertEquals(2, summary.tasksTotal)
        assertEquals(0, summary.tasksCompleted)
        assertEquals(0f, summary.score)
    }

    // ── Catch-up: cierra los días que el worker no llegó a cerrar ─────────────

    @Test
    fun processPendingDays_closesGapUpToYesterday() = runTest {
        val lastClosed = LocalDate(2024, 6, 12)
        val summaryRepo = FakeDaySummaryRepository().also {
            it.insert(DaySummary(
                id = "s1", date = lastClosed, score = 1f,
                tasksTotal = 1, tasksCompleted = 1,                feedbackMessage = ""
            ))
        }
        val processor = makeProcessor(FakeDayTaskRepository(), summaryRepo)

        processor.processPendingDays(today)   // today = 2024-06-15

        val closedDates = summaryRepo.inserted().map { it.date }.toSet()
        assertTrue(LocalDate(2024, 6, 13) in closedDates)
        assertTrue(LocalDate(2024, 6, 14) in closedDates)
        assertTrue(today !in closedDates, "El día en curso no se cierra")
    }

    @Test
    fun processPendingDays_noHistory_doesNothing() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        val processor = makeProcessor(FakeDayTaskRepository(), summaryRepo)

        processor.processPendingDays(today)

        assertTrue(summaryRepo.inserted().isEmpty(), "Sin historial no hay nada que recuperar")
    }
}
