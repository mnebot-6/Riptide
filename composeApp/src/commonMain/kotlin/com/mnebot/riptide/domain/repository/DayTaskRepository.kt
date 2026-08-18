package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.datetime.LocalDate

interface DayTaskRepository {
    suspend fun getByDate(date: LocalDate): List<DayTask>
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<DayTask>
    suspend fun getByBlock(blockId: String): List<DayTask>
    suspend fun getBySourceTask(sourceTaskId: String): List<DayTask>
    suspend fun insert(task: DayTask)
    suspend fun update(task: DayTask)
    suspend fun updateStatus(id: String, status: TaskStatus)
    suspend fun delete(id: String)
    suspend fun deleteBySourceTask(sourceTaskId: String)          // al borrar una recurrente
    suspend fun deleteBySourceId(sourceTaskId: String)
    suspend fun deleteBySourceIdFromDate(sourceTaskId: String, fromDate: LocalDate)
    suspend fun getByDateAndBlock(date: LocalDate, blockId: String): List<DayTask>
    /** sourceIds ya materializados en esa fecha, incluidos los borrados. */
    suspend fun getSourceIdsForDate(date: LocalDate): List<String>
    suspend fun getCompletedRange(from: LocalDate, to: LocalDate): List<DayTask>
    suspend fun countCompletedAllTime(): Int
}