package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.EcosystemStateRepository

class FakeEcosystemStateRepository : EcosystemStateRepository {
    private val states = mutableMapOf<MarineCategory, EcosystemState>()

    fun seed(state: EcosystemState) { states[state.category] = state }
    fun all(): List<EcosystemState> = states.values.toList()

    override suspend fun getByCategory(category: MarineCategory): EcosystemState? = states[category]
    override suspend fun getAll(): List<EcosystemState> = states.values.toList()
    override suspend fun getUnlocked(): List<EcosystemState> = states.values.filter { it.isUnlocked }
    override suspend fun insert(state: EcosystemState) { states[state.category] = state }
    override suspend fun update(state: EcosystemState) { states[state.category] = state }
}
