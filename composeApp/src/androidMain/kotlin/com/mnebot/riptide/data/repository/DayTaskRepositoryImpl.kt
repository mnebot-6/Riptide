package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.DayTaskDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.LocalDate

class DayTaskRepositoryImpl(private val dao: DayTaskDao) : DayTaskRepository {
    override suspend fun getByDate(date: LocalDate): List<DayTask> =
        dao.getByDate(date.toString()).map { it.toDomain() }

    override suspend fun getByBlock(blockId: String): List<DayTask> =
        dao.getByBlock(blockId).map { it.toDomain() }

    override suspend fun getBySourceTask(sourceTaskId: String): List<DayTask> =
        dao.getBySourceTask(sourceTaskId).map { it.toDomain() }

    override suspend fun getPendingBefore(date: LocalDate): List<DayTask> =
        dao.getPendingBefore(date.toString()).map { it.toDomain() }

    override suspend fun insert(task: DayTask) =
        dao.insert(task.toEntity())

    override suspend fun update(task: DayTask) =
        dao.update(task.toEntity())

    override suspend fun updateStatus(id: String, status: TaskStatus) =
        dao.updateStatus(id, status.name)

    override suspend fun delete(id: String) =
        dao.delete(id)

    override suspend fun deleteBySourceTask(sourceTaskId: String) =
        dao.deleteBySourceTask(sourceTaskId)

    override suspend fun deleteBySourceId(sourceTaskId: String) =
        dao.deleteBySourceTask(sourceTaskId)

    override suspend fun deleteBySourceIdFromDate(sourceTaskId: String, fromDate: LocalDate) =
        dao.deleteBySourceTaskFromDate(sourceTaskId, fromDate.toString())

    override suspend fun getByDateAndBlock(date: LocalDate, blockId: String): List<DayTask> =
        dao.getByDateAndBlock(date.toString(), blockId).map { it.toDomain() }

    override suspend fun getCompletedRange(from: LocalDate, to: LocalDate): List<DayTask> =
        dao.getCompletedRange(from.toString(), to.toString()).map { it.toDomain() }
}