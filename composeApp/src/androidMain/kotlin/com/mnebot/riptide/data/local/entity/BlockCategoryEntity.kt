package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "block_categories",
    primaryKeys = ["blockId", "category"],
    foreignKeys = [
        ForeignKey(
            entity = WorkBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BlockCategoryEntity(
    val blockId: String,
    val category: String,
    val updatedAt: String = ""
)