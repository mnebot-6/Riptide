package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object MarineCreaturesTable : Table("marine_creatures") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val ecosystemId = varchar("ecosystem_id", 36)  // Category name or UUID — no FK (mirrors Room)
    val category = varchar("category", 30)        // MarineCategory.name
    val species = varchar("species", 50)           // CreatureSpecies.name
    val nickname = text("nickname").nullable()
    val unlockedAtLevel = integer("unlocked_at_level")
    val experience = integer("experience")
    val creatureLevel = integer("creature_level")
    val unlockedAt = varchar("unlocked_at", 30)    // ISO LocalDateTime
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
