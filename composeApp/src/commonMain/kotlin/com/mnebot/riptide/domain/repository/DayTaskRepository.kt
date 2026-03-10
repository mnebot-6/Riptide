package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.DayTask
import kotlinx.datetime.LocalDate

interface DayTaskRepository {
    suspend fun getByDate(date: LocalDate): List<DayTask>
    suspend fun getByBlock(blockId: String): List<DayTask>
    suspend fun insert(task: DayTask)
    suspend fun update(task: DayTask)
    suspend fun delete(id: String)
}