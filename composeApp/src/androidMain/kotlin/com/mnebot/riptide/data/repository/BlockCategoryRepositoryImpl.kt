package com.mnebot.riptide.data.repository

import com.mnebot.riptide.data.local.dao.BlockCategoryDao
import com.mnebot.riptide.data.local.mapper.toDomain
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.domain.model.BlockCategory
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.BlockCategoryRepository

class BlockCategoryRepositoryImpl(
    private val dao: BlockCategoryDao
) : BlockCategoryRepository {

    override suspend fun getCategoriesForBlock(blockId: String): List<BlockCategory> {
        return dao.getForBlock(blockId).map { it.toDomain() }
    }

    override suspend fun getCategoriesForBlocks(blockIds: List<String>): List<BlockCategory> {
        if (blockIds.isEmpty()) return emptyList()
        return dao.getForBlocks(blockIds).map { it.toDomain() }
    }

    override suspend fun setCategories(blockId: String, categories: List<MarineCategory>) {
        dao.deleteForBlock(blockId)
        val now = nowIso()
        categories.forEach { category ->
            dao.insert(BlockCategory(blockId, category).toEntity().copy(updatedAt = now))
        }
    }

    override suspend fun deleteForBlock(blockId: String) {
        dao.deleteForBlock(blockId)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}