package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate

data class PersonalDate(
    val id: String,
    val title: String,
    val date: LocalDate,
    val type: PersonalDateType,
    val recurrence: PersonalDateRecurrence,
    val notificationsEnabled: Boolean = false
)

enum class PersonalDateType { BIRTHDAY, APPOINTMENT, HOLIDAY, CUSTOM }

enum class PersonalDateRecurrence { ONCE, YEARLY, MONTHLY }
