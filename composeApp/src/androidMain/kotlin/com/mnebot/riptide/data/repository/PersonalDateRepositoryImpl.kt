package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.PersonalDateDao
import com.mnebot.riptide.data.local.entity.PersonalDateEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.PersonalDate
import com.mnebot.riptide.domain.model.PersonalDateRecurrence
import com.mnebot.riptide.domain.model.PersonalDateType
import com.mnebot.riptide.domain.repository.PersonalDateRepository
import kotlinx.datetime.LocalDate

class PersonalDateRepositoryImpl(private val dao: PersonalDateDao) : PersonalDateRepository {

    override suspend fun getAll(): List<PersonalDate> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): PersonalDate? =
        dao.getById(id)?.toDomain()

    override suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<PersonalDate> =
        dao.getByDateRange(from.toString(), to.toString()).map { it.toDomain() }

    override suspend fun insert(date: PersonalDate) {
        dao.insert(date.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    private fun PersonalDateEntity.toDomain() = PersonalDate(
        id = id,
        title = title,
        date = LocalDate.parse(date),
        type = runCatching { PersonalDateType.valueOf(type) }.getOrDefault(PersonalDateType.CUSTOM),
        recurrence = runCatching { PersonalDateRecurrence.valueOf(recurrence) }.getOrDefault(PersonalDateRecurrence.ONCE),
        notificationsEnabled = notificationsEnabled
    )

    private fun PersonalDate.toEntity() = PersonalDateEntity(
        id = id,
        title = title,
        date = date.toString(),
        type = type.name,
        recurrence = recurrence.name,
        notificationsEnabled = notificationsEnabled,
        updatedAt = nowIso()
    )
}
