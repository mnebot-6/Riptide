package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.DaySummaryDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import kotlinx.datetime.LocalDate

class DaySummaryRepositoryImpl(
    private val dao: DaySummaryDao
) : DaySummaryRepository {
    override suspend fun getByDate(date: LocalDate): DaySummary? =
        dao.getByDate(date.toString())?.toDomain()

    override suspend fun getRange(from: LocalDate, to: LocalDate): List<DaySummary> =
        dao.getRange(from.toString(), to.toString()).map { it.toDomain() }

    override suspend fun getAll(): List<DaySummary> =
        dao.getAll().map { it.toDomain() }

    override suspend fun insert(summary: DaySummary) = dao.insert(summary.toEntity().copy(updatedAt = nowIso()))

    override suspend fun getLatestN(n: Int): List<DaySummary> =
        dao.getLatestN(n).map { it.toDomain() }
}