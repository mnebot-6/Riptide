package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.DayTaskDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.LocalDate

class DayTaskRepositoryImpl(private val dao: DayTaskDao) : DayTaskRepository {
    override suspend fun getByDate(date: LocalDate): List<DayTask> =
        dao.getByDate(date.toString()).map { it.toDomain() }

    override suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<DayTask> =
        dao.getByDateRange(from.toString(), to.toString()).map { it.toDomain() }

    override suspend fun getByBlock(blockId: String): List<DayTask> =
        dao.getByBlock(blockId).map { it.toDomain() }

    override suspend fun getBySourceTask(sourceTaskId: String): List<DayTask> =
        dao.getBySourceTask(sourceTaskId).map { it.toDomain() }

    override suspend fun insert(task: DayTask) =
        dao.insert(task.toEntity().copy(updatedAt = nowIso()))

    override suspend fun update(task: DayTask) =
        dao.update(task.toEntity().copy(updatedAt = nowIso()))

    override suspend fun updateStatus(id: String, status: TaskStatus) =
        dao.updateStatus(id, status.name, nowIso())

    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = true, updatedAt = nowIso()))
    }

    override suspend fun deleteBySourceTask(sourceTaskId: String) =
        dao.softDeleteBySourceTask(sourceTaskId, nowIso())

    override suspend fun deleteBySourceId(sourceTaskId: String) =
        dao.softDeleteBySourceTask(sourceTaskId, nowIso())

    override suspend fun deleteBySourceIdFromDate(sourceTaskId: String, fromDate: LocalDate) =
        dao.softDeleteBySourceTaskFromDate(sourceTaskId, fromDate.toString(), nowIso())

    override suspend fun getByDateAndBlock(date: LocalDate, blockId: String): List<DayTask> =
        dao.getByDateAndBlock(date.toString(), blockId).map { it.toDomain() }

    override suspend fun getSourceIdsForDate(date: LocalDate): List<String> =
        dao.getSourceIdsForDate(date.toString())

    override suspend fun getCompletedRange(from: LocalDate, to: LocalDate): List<DayTask> =
        dao.getCompletedRange(from.toString(), to.toString()).map { it.toDomain() }

    override suspend fun countCompletedAllTime(): Int = dao.countCompleted()
}