package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.BlockCategory
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.BlockCategoryRepository

class FakeBlockCategoryRepository : BlockCategoryRepository {
    private val data = mutableMapOf<String, List<MarineCategory>>()

    fun categoriesFor(blockId: String): List<MarineCategory> = data[blockId] ?: emptyList()

    override suspend fun getCategoriesForBlock(blockId: String): List<BlockCategory> =
        (data[blockId] ?: emptyList()).map { BlockCategory(blockId, it) }

    override suspend fun getCategoriesForBlocks(blockIds: List<String>): List<BlockCategory> =
        blockIds.flatMap { getCategoriesForBlock(it) }

    override suspend fun setCategories(blockId: String, categories: List<MarineCategory>) {
        data[blockId] = categories
    }

    override suspend fun deleteForBlock(blockId: String) { data.remove(blockId) }
    override suspend fun deleteAll() { data.clear() }
}
