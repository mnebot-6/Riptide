package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.WorkBlock

interface WorkBlockRepository {
    suspend fun getAll(): List<WorkBlock>
    suspend fun getById(id: String): WorkBlock?
    suspend fun insert(block: WorkBlock)
    suspend fun update(block: WorkBlock)
    suspend fun delete(id: String)
}