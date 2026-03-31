package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.WorkBlockRepository

class FakeWorkBlockRepository : WorkBlockRepository {
    private val blocks = mutableListOf<WorkBlock>()

    override suspend fun getAll(): List<WorkBlock> = blocks.toList()
    override suspend fun getById(id: String): WorkBlock? = blocks.firstOrNull { it.id == id }
    override suspend fun insert(block: WorkBlock) { blocks.add(block) }
    override suspend fun update(block: WorkBlock) {
        val idx = blocks.indexOfFirst { it.id == block.id }
        if (idx >= 0) blocks[idx] = block
    }
    override suspend fun delete(id: String) { blocks.removeAll { it.id == id } }
}
