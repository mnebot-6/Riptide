package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.DaySummary
import kotlinx.datetime.LocalDate

interface DaySummaryRepository {
    suspend fun getByDate(date: LocalDate): DaySummary?
    suspend fun getRange(from: LocalDate, to: LocalDate): List<DaySummary>
    suspend fun insert(summary: DaySummary)
    suspend fun getLatestN(n: Int): List<DaySummary>
}