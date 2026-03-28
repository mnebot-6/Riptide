package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.RecurringTaskDefDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.domain.model.RecurringTaskDef
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository

class RecurringTaskDefRepositoryImpl(private val dao: RecurringTaskDefDao) : RecurringTaskDefRepository {
    override suspend fun getAll(): List<RecurringTaskDef> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): RecurringTaskDef? =
        dao.getById(id)?.toDomain()

    override suspend fun getByBlock(blockId: String): List<RecurringTaskDef> =
        dao.getByBlock(blockId).map { it.toDomain() }

    override suspend fun insert(def: RecurringTaskDef) =
        dao.insert(def.toEntity())

    override suspend fun update(def: RecurringTaskDef) =
        dao.update(def.toEntity())

    override suspend fun delete(id: String) =
        dao.delete(id)
}