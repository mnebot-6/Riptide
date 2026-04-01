package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.WorkBlockDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.WorkBlockRepository

class WorkBlockRepositoryImpl(
    private val dao: WorkBlockDao
) : WorkBlockRepository {
    override suspend fun getAll(): List<WorkBlock> = dao.getAll().map { it.toDomain() }
    override suspend fun getById(id: String): WorkBlock? = dao.getById(id)?.toDomain()
    override suspend fun insert(block: WorkBlock) = dao.insert(block.toEntity().copy(updatedAt = nowIso()))
    override suspend fun update(block: WorkBlock) = dao.update(block.toEntity().copy(updatedAt = nowIso()))
    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(isDeleted = true, updatedAt = nowIso()))
    }
}