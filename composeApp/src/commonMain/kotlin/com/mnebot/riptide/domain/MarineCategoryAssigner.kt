package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository

class MarineCategoryAssigner(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository
) {
    private val allCategories = MarineCategory.entries.toList()

    suspend fun reassign() {
        val blocks = workBlockRepository.getAll()
        if (blocks.isEmpty()) return

        val n = blocks.size
        val total = allCategories.size // 5

        // Construimos un pool circular de categorías que se repite
        // tanto como sea necesario para cubrir todos los bloques
        val pool = mutableListOf<MarineCategory>()
        var i = 0
        repeat(n * total) {
            pool.add(allCategories[i % total])
            i++
        }

        // Calculamos cuántas categorías recibe cada bloque
        val baseCount = total / n       // mínimo por bloque
        val extra = total % n           // primeros 'extra' bloques reciben una más

        var poolIndex = 0
        blocks.forEachIndexed { index, block ->
            val count = if (index < extra) baseCount + 1 else baseCount
            val actualCount = count.coerceAtLeast(1)

            // Tomamos categorías del pool evitando repeticiones dentro del mismo bloque
            val assigned = mutableListOf<MarineCategory>()
            val seen = mutableSetOf<MarineCategory>()
            var attempts = 0
            while (assigned.size < actualCount && attempts < allCategories.size * 2) {
                val candidate = allCategories[poolIndex % total]
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