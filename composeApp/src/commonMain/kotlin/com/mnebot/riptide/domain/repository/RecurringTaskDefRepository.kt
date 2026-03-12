package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.RecurringTaskDef

interface RecurringTaskDefRepository {
    suspend fun getAll(): List<RecurringTaskDef>
    suspend fun getById(id: String): RecurringTaskDef?
    suspend fun getByBlock(blockId: String): List<RecurringTaskDef>
    suspend fun insert(def: RecurringTaskDef)
    suspend fun update(def: RecurringTaskDef)
    suspend fun delete(id: String)
}