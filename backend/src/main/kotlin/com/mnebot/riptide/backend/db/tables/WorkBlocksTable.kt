package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object WorkBlocksTable : Table("work_blocks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val name = text("name")
    val color = varchar("color", 10)                // "#RRGGBB"
    val icon = varchar("icon", 50)
    val recurrenceJson = text("recurrence_json")     // kotlinx.serialization JSON
    val isActive = bool("is_active").default(true)
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val isDeleted = bool("is_deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}
