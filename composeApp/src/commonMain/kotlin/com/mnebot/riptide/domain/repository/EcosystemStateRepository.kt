package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory

interface EcosystemStateRepository {
    suspend fun getByCategory(category: MarineCategory): EcosystemState?
    suspend fun update(state: EcosystemState)
}