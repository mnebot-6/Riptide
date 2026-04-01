package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DaySummariesTable : Table("day_summaries") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val date = varchar("date", 10)              // ISO LocalDate
    val score = float("score")
    val tasksTotal = integer("tasks_total")
    val tasksCompleted = integer("tasks_completed")
    val streakDay = integer("streak_day")
    val feedbackMessage = text("feedback_message")
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
