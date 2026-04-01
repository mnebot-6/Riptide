package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.MarineCreatureDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.repository.MarineCreatureRepository

class MarineCreatureRepositoryImpl(
    private val dao: MarineCreatureDao
) : MarineCreatureRepository {
    override suspend fun getByEcosystem(ecosystemId: String): List<MarineCreature> =
        dao.getByEcosystem(ecosystemId).map { it.toDomain() }

    override suspend fun getByCategory(category: MarineCategory): List<MarineCreature> =
        dao.getByCategory(category.name).map { it.toDomain() }

    override suspend fun insert(creature: MarineCreature) = dao.insert(creature.toEntity().copy(updatedAt = nowIso()))
    override suspend fun update(creature: MarineCreature) = dao.update(creature.toEntity().copy(updatedAt = nowIso()))
}