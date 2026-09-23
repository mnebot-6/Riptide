package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.fakes.FakeRecurringTaskDefRepository
import com.mnebot.riptide.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurringTaskGeneratorTest {

    private val monday = LocalDate(2024, 6, 17)     // Lunes (isoDayNumber = 1)
    private val tuesday = LocalDate(2024, 6, 18)    // Martes (isoDayNumber = 2)
    private val wednesday = LocalDate(2024, 6, 19)   // Miercoles (isoDayNumber = 3)

    private fun generator(
        defRepo: FakeRecurringTaskDefRepository = FakeRecurringTaskDefRepository(),
        taskRepo: FakeDayTaskRepository = FakeDayTaskRepository()
    ) = RecurringTaskGenerator(defRepo, taskRepo) to taskRepo

    private fun weeklyDef(
        id: String = "def1",
        blockId: String = "block1",
        title: String = "Weekly task",
        time: LocalTime? = LocalTime(9, 0),
        days: List<Int> = listOf(1, 3, 5), // Lun, Mie, Vie
        isActive: Boolean = true,
        notificationsEnabled: Boolean = false
    ) = RecurringTaskDef(
        id = id,
        blockId = blockId,
        title = title,
        time = time,
        recurrence = Recurrence.Weekly(days.map { WeeklySlot(it, null, null) }),
        isActive = isActive,
        notificationsEnabled = notificationsEnabled
    )

    // ── Id determinista (deduplicacion entre dispositivos) ───────────────────

    @Test
    fun instance_id_is_deterministic_across_devices() = runTest {
        val defA = FakeRecurringTaskDefRepository()
        defA.insert(weeklyDef(days = listOf(1)))
        val (genA, repoA) = generator(defA)
        genA.generateUpTo(monday, daysAhead = 0)

        // Otro dispositivo, base de datos vacia, misma definicion y mismo dia.
        val defB = FakeRecurringTaskDefRepository()
        defB.insert(weeklyDef(days = listOf(1)))
        val (genB, repoB) = generator(defB)
        genB.generateUpTo(monday, daysAhead = 0)

        assertEquals(
            repoA.getByDate(monday).single().id,
            repoB.getByDate(monday).single().id,
            "Ids distintos para la misma (definicion, dia) duplican la tarea al sincronizar"
        )
        // El backend rechaza el sync entero si el id no tiene forma de UUID.
        assertTrue(
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
                .matches(repoA.getByDate(monday).single().id)
        )
    }

    @Test
    fun instance_id_is_stable_value() {
        // Fijado: si cambia el algoritmo, las instancias ya creadas se duplican en el sync.
        val (gen, _) = generator()
        assertEquals(
            "a279adb3-ba7f-c7b8-878e-d1b60d4fd468",
            gen.instanceId("0e3b589e-3554-46c8-b35e-4d3513af7f23", LocalDate(2026, 9, 23))
        )
    }

    // ── Generacion basica ────────────────────────────────────────────────────

    @Test
    fun generates_task_on_matching_weekday() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(days = listOf(1))) // Solo lunes

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0) // Solo el lunes

        val tasks = taskRepo.getByDate(monday)
        assertEquals(1, tasks.size)
        assertEquals("Weekly task", tasks[0].title)
        assertEquals("block1", tasks[0].blockId)
        assertEquals("def1", tasks[0].sourceTaskId)
    }

    @Test
    fun skips_non_matching_weekday() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(days = listOf(1))) // Solo lunes

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(tuesday, daysAhead = 0) // Solo martes

        val tasks = taskRepo.getByDate(tuesday)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun generates_for_multiple_days_ahead() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(days = listOf(1, 3))) // Lun + Mie

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 2) // Lun, Mar, Mie

        val monTasks = taskRepo.getByDate(monday)
        val tueTasks = taskRepo.getByDate(tuesday)
        val wedTasks = taskRepo.getByDate(wednesday)

        assertEquals(1, monTasks.size, "Lunes deberia tener 1 tarea")
        assertEquals(0, tueTasks.size, "Martes no deberia tener tareas")
        assertEquals(1, wedTasks.size, "Miercoles deberia tener 1 tarea")
    }

    // ── No duplicados ────────────────────────────────────────────────────────

    @Test
    fun no_duplicate_if_already_generated() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        val taskRepo = FakeDayTaskRepository()
        defRepo.insert(weeklyDef(days = listOf(1)))

        val gen = RecurringTaskGenerator(defRepo, taskRepo)

        gen.generateUpTo(monday, daysAhead = 0)
        assertEquals(1, taskRepo.getByDate(monday).size)

        // Ejecutar de nuevo — no deberia duplicar
        gen.generateUpTo(monday, daysAhead = 0)
        assertEquals(1, taskRepo.getByDate(monday).size)
    }

    // ── Inactivas ────────────────────────────────────────────────────────────

    @Test
    fun skips_inactive_definitions() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(isActive = false, days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        assertTrue(taskRepo.getByDate(monday).isEmpty())
    }

    // ── Recurrence.None ──────────────────────────────────────────────────────

    @Test
    fun recurrence_none_never_generates() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(
            RecurringTaskDef(
                id = "def-none", blockId = "block1", title = "No recurrence",
                time = LocalTime(10, 0), recurrence = Recurrence.None,
                isActive = true
            )
        )

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 6)

        // Ningun dia deberia tener tareas
        for (i in 0..6) {
            assertTrue(taskRepo.getByDate(LocalDate(2024, 6, 17 + i)).isEmpty())
        }
    }

    // ── Hora opcional ────────────────────────────────────────────────────────

    @Test
    fun propagates_null_time_to_generated_task() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(time = null, days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val task = taskRepo.getByDate(monday).first()
        val schedule = task.schedule as TaskSchedule.OneTime
        assertEquals(null, schedule.time)
    }

    @Test
    fun propagates_time_to_generated_task() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(time = LocalTime(14, 30), days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val task = taskRepo.getByDate(monday).first()
        val schedule = task.schedule as TaskSchedule.OneTime
        assertEquals(LocalTime(14, 30), schedule.time)
    }

    // ── notificationsEnabled ─────────────────────────────────────────────────

    @Test
    fun propagates_notificationsEnabled_true() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(notificationsEnabled = true, days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val task = taskRepo.getByDate(monday).first()
        assertTrue(task.notificationsEnabled)
    }

    @Test
    fun propagates_notificationsEnabled_false() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(notificationsEnabled = false, days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val task = taskRepo.getByDate(monday).first()
        assertEquals(false, task.notificationsEnabled)
    }

    // ── Multiples definiciones ───────────────────────────────────────────────

    @Test
    fun handles_multiple_definitions_same_day() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(id = "def1", title = "Task A", days = listOf(1)))
        defRepo.insert(weeklyDef(id = "def2", title = "Task B", days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val tasks = taskRepo.getByDate(monday)
        assertEquals(2, tasks.size)
        assertEquals(setOf("Task A", "Task B"), tasks.map { it.title }.toSet())
    }

    @Test
    fun generated_task_has_pending_status() = runTest {
        val defRepo = FakeRecurringTaskDefRepository()
        defRepo.insert(weeklyDef(days = listOf(1)))

        val (gen, taskRepo) = generator(defRepo)
        gen.generateUpTo(monday, daysAhead = 0)

        val task = taskRepo.getByDate(monday).first()
        assertEquals(TaskStatus.PENDING, task.status)
        assertEquals(false, task.hasBeenRewarded)
    }
}
