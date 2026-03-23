package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.LocalDate

class FakeDayTaskRepository : DayTaskRepository {

    private val tasks = mutableListOf<DayTask>()

    fun addTask(task: DayTask) { tasks.add(task) }

    override suspend fun getByDate(date: LocalDate): List<DayTask> =
        tasks.filter { task ->
            when (val s = task.schedule) {
                is com.mnebot.riptide.domain.model.TaskSchedule.OneTime -> s.date == date
                is com.mnebot.riptide.domain.model.TaskSchedule.Recurring -> true
            }
        }

    override suspend fun getByDateAndBlock(date: LocalDate, blockId: String): List<DayTask> =
        getByDate(date).filter { it.blockId == blockId }

    override suspend fun updateStatus(id: String, status: TaskStatus) {
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx >= 0) tasks[idx] = tasks[idx].copy(status = status)
    }

    override suspend fun getByBlock(blockId: String): List<DayTask> =
        tasks.filter { it.blockId == blockId }

    override suspend fun getBySourceTask(sourceTaskId: String): List<DayTask> =
        tasks.filter { it.sourceTaskId == sourceTaskId }

    override suspend fun getPendingBefore(date: LocalDate): List<DayTask> = emptyList()
    override suspend fun insert(task: DayTask) { tasks.add(task) }
    override suspend fun update(task: DayTask) {
        val idx = tasks.indexOfFirst { it.id == task.id }
        if (idx >= 0) tasks[idx] = task
    }
    override suspend fun delete(id: String) { tasks.removeAll { it.id == id } }
    override suspend fun deleteBySourceTask(sourceTaskId: String) {}
    override suspend fun deleteBySourceId(sourceTaskId: String) {}
    override suspend fun deleteBySourceIdFromDate(sourceTaskId: String, fromDate: LocalDate) {}
    override suspend fun getCompletedRange(from: LocalDate, to: LocalDate): List<DayTask> = emptyList()
}
