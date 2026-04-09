package com.mnebot.riptide.domain

import kotlinx.datetime.LocalDate

data class Holiday(
    val date: LocalDate,
    val name: String
)

/** Hardcoded holiday calendars by country. */
object HolidayCalendar {

    fun getHolidays(country: String, year: Int): List<Holiday> = when (country) {
        "ES" -> spainHolidays(year)
        else -> emptyList()
    }

    private fun spainHolidays(year: Int): List<Holiday> = listOf(
        Holiday(LocalDate(year, 1, 1), "Ano Nuevo"),
        Holiday(LocalDate(year, 1, 6), "Dia de Reyes"),
        Holiday(LocalDate(year, 5, 1), "Dia del Trabajador"),
        Holiday(LocalDate(year, 8, 15), "Asuncion de la Virgen"),
        Holiday(LocalDate(year, 10, 12), "Fiesta Nacional"),
        Holiday(LocalDate(year, 11, 1), "Todos los Santos"),
        Holiday(LocalDate(year, 12, 6), "Dia de la Constitucion"),
        Holiday(LocalDate(year, 12, 8), "Inmaculada Concepcion"),
        Holiday(LocalDate(year, 12, 25), "Navidad")
    )
}
