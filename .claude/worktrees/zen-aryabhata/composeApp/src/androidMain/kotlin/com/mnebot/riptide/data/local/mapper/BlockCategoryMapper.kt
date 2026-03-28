package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.BlockCategoryEntity
import com.mnebot.riptide.domain.model.BlockCategory
import com.mnebot.riptide.domain.model.MarineCategory

fun BlockCategoryEntity.toDomain(): BlockCategory {
    return BlockCategory(
        blockId = blockId,
        category = MarineCategory.valueOf(category)
    )
}

fun BlockCategory.toEntity(): BlockCategoryEntity {
    return BlockCategoryEntity(
        blockId = blockId,
        category = category.name
    )
}