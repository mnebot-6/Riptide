package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.DaySummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DayStreakTest {

    private fun summary(day: Int, score: Float, total: Int = 5) = DaySummary(
        id = "s$day",
        date = LocalDate(2026, 3, day),
        score = score,
        tasksTotal = total,
        tasksCompleted = (total * score).toInt(),
        feedbackMessage = ""
    )

    private fun at(day: Int) = LocalDate(2026, 3, day)

    @Test
    fun countsConsecutiveAchievedDays() {
        val summaries = listOf(summary(10, 1.0f), summary(11, 0.8f), summary(12, 1.0f))
        assertEquals(3, DayStreak.currentFrom(summaries, at(12)))
    }

    @Test
    fun belowThresholdBreaksStreak() {
        val summaries = listOf(summary(10, 1.0f), summary(11, 0.79f), summary(12, 1.0f))
        assertEquals(1, DayStreak.currentFrom(summaries, at(12)))
    }

    @Test
    fun neutralDayIsSkipped_neitherAddsNorBreaks() {
        val summaries = listOf(
            summary(10, 1.0f),
            summary(11, 0f, total = 0),   // día sin tareas
            summary(12, 1.0f)
        )
        assertEquals(2, DayStreak.currentFrom(summaries, at(12)))
    }

    @Test
    fun missingSummaryBreaksStreak() {
        // El día 11 no se cerró: la racha no puede saltar por encima
        val summaries = listOf(summary(10, 1.0f), summary(12, 1.0f))
        assertEquals(1, DayStreak.currentFrom(summaries, at(12)))
    }

    @Test
    fun emptyHistoryIsZero() {
        assertEquals(0, DayStreak.currentFrom(emptyList(), at(12)))
    }

    @Test
    fun longestFindsBestRunNotTheCurrentOne() {
        val summaries = listOf(
            summary(1, 1.0f), summary(2, 1.0f), summary(3, 1.0f), summary(4, 1.0f),
            summary(5, 0.2f),
            summary(6, 1.0f), summary(7, 1.0f)
        )
        assertEquals(4, DayStreak.longest(summaries))
    }

    @Test
    fun longestSkipsNeutralDaysWithoutBreaking() {
        val summaries = listOf(
            summary(1, 1.0f),
            summary(2, 0f, total = 0),
            summary(3, 1.0f)
        )
        assertEquals(2, DayStreak.longest(summaries))
    }
}
