package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.DayTaskDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.repository.DayTaskRepository
import kotlinx.datetime.LocalDate

class DayTaskRepositoryImpl(
    private val dao: DayTaskDao
) : DayTaskRepository {
    override suspend fun getByDate(date: LocalDate): List<DayTask> =
        dao.getByDate(date.toString()).map { it.toDomain() }
    override suspend fun getByBlock(blockId: String): List<DayTask> =
        dao.getByBlock(blockId).map { it.toDomain() }
    override suspend fun insert(task: DayTask) = dao.insert(task.toEntity())
    override suspend fun update(task: DayTask) = dao.update(task.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}