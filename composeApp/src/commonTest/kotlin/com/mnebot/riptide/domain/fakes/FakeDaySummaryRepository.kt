package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import kotlinx.datetime.LocalDate

class FakeDaySummaryRepository : DaySummaryRepository {

    private val summaries = mutableMapOf<LocalDate, DaySummary>()

    fun inserted(): List<DaySummary> = summaries.values.toList()

    override suspend fun getByDate(date: LocalDate): DaySummary? = summaries[date]
    override suspend fun insert(summary: DaySummary) { summaries[summary.date] = summary }
    override suspend fun getRange(from: LocalDate, to: LocalDate): List<DaySummary> =
        summaries.values.filter { it.date in from..to }

    override suspend fun getLatestN(n: Int): List<DaySummary> =
        summaries.values.sortedByDescending { it.date }.take(n)
}
