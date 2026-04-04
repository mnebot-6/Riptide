package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object RecurringTaskDefsTable : Table("recurring_task_defs") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val blockId = varchar("block_id", 36).references(WorkBlocksTable.id)
    val title = text("title")
    val time = varchar("time", 8).nullable()    // ISO LocalTime, nullable
    val recurrence = text("recurrence")          // JSON
    val isActive = bool("is_active").default(true)
    val notificationsEnabled = bool("notifications_enabled").default(false)
    val targetCount = integer("target_count").nullable()
    val noteTemplate = text("note_template").nullable()
    val timerDurationMinutes = integer("timer_duration_minutes").nullable()
    val isPriority = bool("is_priority").default(false)
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}
