package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object BlockCategoriesTable : Table("block_categories") {
    val blockId = varchar("block_id", 36).references(WorkBlocksTable.id)
    val category = varchar("category", 30)  // MarineCategory.name
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(blockId, category)
}
