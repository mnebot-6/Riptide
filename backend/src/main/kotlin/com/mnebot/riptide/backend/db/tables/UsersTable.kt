package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UsersTable : Table("users") {
    val id = varchar("id", 36)              // UUID string
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 255).nullable()
    val googleId = varchar("google_id", 255).uniqueIndex()
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
