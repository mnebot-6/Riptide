package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository

class MarineCategoryAssigner(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository,
    private val ecosystemStateRepository: EcosystemStateRepository
) {
    suspend fun reassign() {
        val blocks = workBlockRepository.getAll()
        if (blocks.isEmpty()) return

        // Solo categorías desbloqueadas y que no son DECORATION
        val unlockedCategories = ecosystemStateRepository.getUnlocked()
            .map { it.category }
            .filter { it != MarineCategory.DECORATION }

        if (unlockedCategories.isEmpty()) return

        val n = blocks.size
        val total = unlockedCategories.size

        val baseCount = total / n
        val extra = total % n

        var poolIndex = 0
        blocks.forEachIndexed { index, block ->
            val count = (if (index < extra) baseCount + 1 else baseCount).coerceAtLeast(1)

            val assigned = mutableListOf<MarineCategory>()
            val seen = mutableSetOf<MarineCategory>()
            var attempts = 0
            while (assigned.size < count && attempts < total * 2) {
                val candidate = unlockedCategories[poolIndex % total]
                poolIndex++
                attempts++
                if (!seen.contains(candidate)) {
                    assigned.add(candidate)
                    seen.add(candidate)
                }
            }

            blockCategoryRepository.setCategories(block.id, assigned)
        }
    }
}