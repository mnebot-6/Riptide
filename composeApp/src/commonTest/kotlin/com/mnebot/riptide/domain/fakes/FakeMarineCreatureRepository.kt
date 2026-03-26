package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.repository.MarineCreatureRepository

class FakeMarineCreatureRepository : MarineCreatureRepository {
    private val creatures = mutableListOf<MarineCreature>()

    fun all(): List<MarineCreature> = creatures.toList()

    override suspend fun getByEcosystem(ecosystemId: String): List<MarineCreature> =
        creatures.filter { it.ecosystemId == ecosystemId }

    override suspend fun getByCategory(category: MarineCategory): List<MarineCreature> =
        creatures.filter { it.species.category == category }

    override suspend fun insert(creature: MarineCreature) { creatures.add(creature) }

    override suspend fun update(creature: MarineCreature) {
        val idx = creatures.indexOfFirst { it.id == creature.id }
        if (idx >= 0) creatures[idx] = creature
    }
}
