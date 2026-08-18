package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository

/**
 * Asigna categorías marinas a los bloques que aún no tienen ninguna.
 *
 * NO reasigna las existentes: si el mapa bloque→categoría cambiara al crear o
 * editar cualquier bloque, el usuario vería crecer partes distintas del ecosistema
 * sin haber cambiado nada de su rutina.
 */
class MarineCategoryAssigner(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository,
    private val ecosystemStateRepository: EcosystemStateRepository
) {
    suspend fun assignMissing() {
        val blocks = workBlockRepository.getAll()
        if (blocks.isEmpty()) return

        val existing = blockCategoryRepository.getCategoriesForBlocks(blocks.map { it.id })
        val assignedBlockIds = existing.map { it.blockId }.toSet()
        val pending = blocks.filter { it.id !in assignedBlockIds }
        if (pending.isEmpty()) return

        val pool = ecosystemStateRepository.getUnlocked()
            .map { it.category }
            .filter { it != MarineCategory.DECORATION && it != MarineCategory.COMPANION }
        if (pool.isEmpty()) return

        // Reparto round-robin sobre el pool, arrancando donde lo dejaron los bloques ya
        // asignados para no dar siempre la misma categoría a los bloques nuevos.
        var poolIndex = existing.size
        val perBlock = (pool.size / blocks.size).coerceAtLeast(1)

        pending.forEach { block ->
            val assigned = LinkedHashSet<MarineCategory>()
            var attempts = 0
            while (assigned.size < perBlock && attempts < pool.size) {
                assigned += pool[poolIndex % pool.size]
                poolIndex++
                attempts++
            }
            blockCategoryRepository.setCategories(block.id, assigned.toList())
        }
    }
}
