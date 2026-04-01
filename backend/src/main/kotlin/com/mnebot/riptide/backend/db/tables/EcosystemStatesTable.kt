package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object EcosystemStatesTable : Table("ecosystem_states") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val category = varchar("category", 30)       // MarineCategory.name
    val totalExperience = integer("total_experience")
    val currentLevel = integer("current_level")
    val isUnlocked = bool("is_unlocked")
    val lastUpdated = varchar("last_updated", 30) // ISO LocalDateTime
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
