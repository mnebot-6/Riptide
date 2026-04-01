package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object BlockStreaksTable : Table("block_streaks") {
    val blockId = varchar("block_id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val currentStreak = integer("current_streak")
    val longestStreak = integer("longest_streak").default(0)
    val lastActiveDate = varchar("last_active_date", 10)   // ISO LocalDate
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    // Composite PK — prevents collision between users with same blockId
    override val primaryKey = PrimaryKey(blockId, userId)
}
