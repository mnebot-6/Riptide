package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory

interface EcosystemStateRepository {
    suspend fun getByCategory(category: MarineCategory): EcosystemState?
    suspend fun getAll(): List<EcosystemState>
    suspend fun getUnlocked(): List<EcosystemState>
    suspend fun insert(state: EcosystemState)
    suspend fun update(state: EcosystemState)
}