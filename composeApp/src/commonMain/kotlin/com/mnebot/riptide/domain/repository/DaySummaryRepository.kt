package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.DaySummary
import kotlinx.datetime.LocalDate

interface DaySummaryRepository {
    suspend fun getByDate(date: LocalDate): DaySummary?
    suspend fun insert(summary: DaySummary)
}