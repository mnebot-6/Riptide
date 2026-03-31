package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.RecurringTaskDef
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository

class FakeRecurringTaskDefRepository : RecurringTaskDefRepository {
    private val defs = mutableListOf<RecurringTaskDef>()

    override suspend fun getAll(): List<RecurringTaskDef> = defs.toList()
    override suspend fun getById(id: String): RecurringTaskDef? = defs.firstOrNull { it.id == id }
    override suspend fun getByBlock(blockId: String): List<RecurringTaskDef> =
        defs.filter { it.blockId == blockId }
    override suspend fun insert(def: RecurringTaskDef) { defs.add(def) }
    override suspend fun update(def: RecurringTaskDef) {
        val idx = defs.indexOfFirst { it.id == def.id }
        if (idx >= 0) defs[idx] = def
    }
    override suspend fun delete(id: String) { defs.removeAll { it.id == id } }
}
