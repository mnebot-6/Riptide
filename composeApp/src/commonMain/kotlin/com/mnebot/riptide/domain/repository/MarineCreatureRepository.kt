package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.model.MarineCategory

interface MarineCreatureRepository {
    suspend fun getByEcosystem(ecosystemId: String): List<MarineCreature>
    suspend fun getByCategory(category: MarineCategory): List<MarineCreature>
    suspend fun insert(creature: MarineCreature)
    suspend fun update(creature: MarineCreature)
}