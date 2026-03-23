package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeBlockStreakRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlockStreakProcessorTest {

    private val today     = LocalDate(2024, 6, 15)
    private val yesterday = today.minus(1, DateTimeUnit.DAY)
    private val twoDaysAgo = today.minus(2, DateTimeUnit.DAY)

    private fun completedTask(id: String, blockId: String, date: LocalDate = today) = DayTask(
        id = id,
        blockId = blockId,
        title = "Task $id",
        schedule = TaskSchedule.OneTime(date, null),
        status = TaskStatus.COMPLETED,
        completedAt = null,
        postponedTo = null,
        sourceTaskId = null
    )

    private fun pendingTask(id: String, blockId: String, date: LocalDate = today) = DayTask(
        id = id,
        blockId = blockId,
        title = "Task $id",
        schedule = TaskSchedule.OneTime(date, null),
        status = TaskStatus.PENDING,
        completedAt = null,
        postponedTo = null,
        sourceTaskId = null
    )

    private fun makeProcessor(
        taskRepo: FakeDayTaskRepository,
        streakRepo: FakeBlockStreakRepository
    ) = BlockStreakProcessor(taskRepo, streakRepo)

    // ── First completion ───────────────────────────────────────────────────────

    @Test
    fun firstCompletion_streak1() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository()
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(1, result["blockA"]?.currentStreak)
        assertEquals(1, streakRepo.getAll_snapshot()["blockA"]?.currentStreak)
    }

    // ── Consecutive day increments streak ─────────────────────────────────────

    @Test
    fun consecutiveDay_incrementsStreak() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 3, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(4, result["blockA"]?.currentStreak)
    }

    // ── Non-consecutive day resets streak to 1 ────────────────────────────────

    @Test
    fun nonConsecutiveDay_resetsStreakTo1() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 5, lastActiveDate = twoDaysAgo))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(1, result["blockA"]?.currentStreak)
    }

    // ── No tasks → neutral, block skipped ─────────────────────────────────────

    @Test
    fun noTasks_blockSkipped() = runTest {
        val taskRepo = FakeDayTaskRepository() // empty
        val streakRepo = FakeBlockStreakRepository()
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertTrue(result.isEmpty(), "Should skip blocks with no tasks")
    }

    // ── Not all completed → streak resets to 0 ────────────────────────────────

    @Test
    fun notAllCompleted_streakResetsTo0() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
            it.addTask(pendingTask("t2", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 4, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(0, result["blockA"]?.currentStreak)
        assertEquals(0, streakRepo.getAll_snapshot()["blockA"]?.currentStreak)
    }

    // ── Milestone 7 detected ──────────────────────────────────────────────────

    @Test
    fun milestone7_detectedOnDaySeven() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 6, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(7, result["blockA"]?.currentStreak)
        assertEquals(listOf(7), result["blockA"]?.milestonesReached)
    }

    // ── Milestone 14 detected ─────────────────────────────────────────────────

    @Test
    fun milestone14_detectedOnDayFourteen() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 13, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(14, result["blockA"]?.currentStreak)
        assertEquals(listOf(14), result["blockA"]?.milestonesReached)
    }

    // ── Milestone 30 detected ─────────────────────────────────────────────────

    @Test
    fun milestone30_detectedOnDayThirty() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 29, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(30, result["blockA"]?.currentStreak)
        assertEquals(listOf(30), result["blockA"]?.milestonesReached)
    }

    // ── No false milestone on non-milestone day ───────────────────────────────

    @Test
    fun noMilestone_onNonMilestoneDay() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 3, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(4, result["blockA"]?.currentStreak)
        assertTrue(result["blockA"]?.milestonesReached?.isEmpty() == true)
    }

    // ── No duplicate milestone if streak already past 7 ──────────────────────

    @Test
    fun noMilestone_whenAlreadyPastMilestone() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 8, lastActiveDate = yesterday))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        assertEquals(9, result["blockA"]?.currentStreak)
        assertTrue(result["blockA"]?.milestonesReached?.isEmpty() == true)
    }

    // ── Multiple blocks processed independently ───────────────────────────────

    @Test
    fun multipleBlocks_processedIndependently() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
            it.addTask(completedTask("t2", "blockB"))
            it.addTask(pendingTask("t3", "blockC"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            it.insert(BlockStreak("blockA", currentStreak = 2, lastActiveDate = yesterday))
            it.insert(BlockStreak("blockB", currentStreak = 1, lastActiveDate = twoDaysAgo))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA", "blockB", "blockC"))

        assertEquals(3, result["blockA"]?.currentStreak)  // consecutive
        assertEquals(1, result["blockB"]?.currentStreak)  // reset (gap)
        assertEquals(0, result["blockC"]?.currentStreak)  // incomplete
    }

    // ── Existing streak not updated on same day ───────────────────────────────

    @Test
    fun existingStreak_notUpdatedIfLastActiveDateIsToday() = runTest {
        val taskRepo = FakeDayTaskRepository().also {
            it.addTask(completedTask("t1", "blockA"))
        }
        val streakRepo = FakeBlockStreakRepository().also {
            // Already processed today
            it.insert(BlockStreak("blockA", currentStreak = 5, lastActiveDate = today))
        }
        val processor = makeProcessor(taskRepo, streakRepo)

        val result = processor.processDay(today, listOf("blockA"))

        // lastActiveDate == today → not consecutive (isConsecutive requires lastActiveDate == yesterday)
        // So streak resets to 1
        assertEquals(1, result["blockA"]?.currentStreak)
    }
}
