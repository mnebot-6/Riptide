package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.PersonalDate
import kotlinx.datetime.LocalDate

interface PersonalDateRepository {
    suspend fun getAll(): List<PersonalDate>
    suspend fun getById(id: String): PersonalDate?
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<PersonalDate>
    suspend fun insert(date: PersonalDate)
    suspend fun delete(id: String)
}
