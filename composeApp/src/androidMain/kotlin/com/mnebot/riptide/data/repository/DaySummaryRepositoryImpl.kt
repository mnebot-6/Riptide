package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.DaySummaryDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import kotlinx.datetime.LocalDate

class DaySummaryRepositoryImpl(
    private val dao: DaySummaryDao
) : DaySummaryRepository {
    override suspend fun getByDate(date: LocalDate): DaySummary? =
        dao.getByDate(date.toString())?.toDomain()
    override suspend fun insert(summary: DaySummary) = dao.insert(summary.toEntity())
}