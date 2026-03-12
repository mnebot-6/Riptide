package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.datetime.LocalDate

interface DayTaskRepository {
    suspend fun getByDate(date: LocalDate): List<DayTask>
    suspend fun getByBlock(blockId: String): List<DayTask>
    suspend fun getBySourceTask(sourceTaskId: String): List<DayTask>
    suspend fun getPendingBefore(date: LocalDate): List<DayTask>  // para expirar en resumen nocturno
    suspend fun insert(task: DayTask)
    suspend fun update(task: DayTask)
    suspend fun updateStatus(id: String, status: TaskStatus)
    suspend fun delete(id: String)
    suspend fun deleteBySourceTask(sourceTaskId: String)          // al borrar una recurrente
    suspend fun deleteBySourceId(sourceTaskId: String)
    suspend fun deleteBySourceIdFromDate(sourceTaskId: String, fromDate: LocalDate)
}