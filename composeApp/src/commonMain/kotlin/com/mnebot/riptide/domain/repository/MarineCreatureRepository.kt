package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.MarineCreature

interface MarineCreatureRepository {
    suspend fun getByEcosystem(ecosystemId: String): List<MarineCreature>
    suspend fun insert(creature: MarineCreature)
    suspend fun update(creature: MarineCreature)
}