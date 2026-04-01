package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DayTasksTable : Table("day_tasks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val blockId = varchar("block_id", 36).nullable()  // FK SET NULL on block delete
    val title = text("title")
    val scheduleType = varchar("schedule_type", 20)    // "ONE_TIME" | "RECURRING"
    val date = varchar("date", 10).nullable()          // ISO LocalDate, solo ONE_TIME
    val time = varchar("time", 8).nullable()           // ISO LocalTime, nullable
    val recurrence = text("recurrence").nullable()      // JSON, solo RECURRING
    val status = varchar("status", 20)                  // TaskStatus.name
    val completedAt = varchar("completed_at", 30).nullable()   // ISO LocalDateTime
    val postponedTo = varchar("postponed_to", 30).nullable()   // ISO LocalDateTime
    val sourceTaskId = varchar("source_task_id", 36).nullable()
    val hasBeenRewarded = bool("has_been_rewarded").default(false)
    val notificationsEnabled = bool("notifications_enabled").default(false)
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}
