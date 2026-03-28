package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.BlockCategory
import com.mnebot.riptide.domain.model.MarineCategory

interface BlockCategoryRepository {
    suspend fun getCategoriesForBlock(blockId: String): List<BlockCategory>
    suspend fun getCategoriesForBlocks(blockIds: List<String>): List<BlockCategory>
    suspend fun setCategories(blockId: String, categories: List<MarineCategory>)
    suspend fun deleteForBlock(blockId: String)
    suspend fun deleteAll()
}