package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.EcosystemStateDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.EcosystemStateRepository

class EcosystemStateRepositoryImpl(
    private val dao: EcosystemStateDao
) : EcosystemStateRepository {
    override suspend fun getByCategory(category: MarineCategory): EcosystemState? =
        dao.getByCategory(category.name)?.toDomain()

    override suspend fun getAll(): List<EcosystemState> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getUnlocked(): List<EcosystemState> =
        dao.getUnlocked().map { it.toDomain() }

    override suspend fun update(state: EcosystemState) = dao.update(state.toEntity().copy(updatedAt = nowIso()))
    override suspend fun insert(state: EcosystemState) = dao.insert(state.toEntity().copy(updatedAt = nowIso()))
}