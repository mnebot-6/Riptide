package com.mnebot.riptide.widget

import com.mnebot.riptide.data.local.entity.DayTaskEntity

object WidgetTaskMutator {

    /**
     * [generatedAtIso] resella el snapshot: si no, `provideGlance` lo ve viejo y
     * recarga de Room antes de que el clic se haya persistido, deshaciendo el cambio
     * en pantalla.
     */
    fun toggleInSnapshot(snapshot: WidgetSnapshot, taskId: String, generatedAtIso: String): WidgetSnapshot {
        val updatedItems = snapshot.items.map { item ->
            if (item.id != taskId) item else applyClick(item)
        }
        return snapshot.copy(
            items = WidgetTaskSorter.sort(updatedItems),
            generatedAtIso = generatedAtIso
        )
    }

    fun applyClick(item: WidgetTaskItem): WidgetTaskItem {
        val isCountable = item.isCountable && item.targetCount != null && item.targetCount > 0
        return when {
            item.isCompleted && isCountable ->
                item.copy(isCompleted = false, currentCount = 0)
            item.isCompleted ->
                item.copy(isCompleted = false)
            isCountable -> {
                val newCount = item.currentCount + 1
                if (newCount >= item.targetCount) {
                    item.copy(isCompleted = true, currentCount = newCount)
                } else {
                    item.copy(currentCount = newCount)
                }
            }
            else -> item.copy(isCompleted = true)
        }
    }

    /**
     * [completedAtIso] es hora local (se muestra al usuario); [updatedAtIso] es UTC
     * porque es lo que compara el servidor al resolver conflictos de sync.
     */
    fun applyClickToEntity(
        entity: DayTaskEntity,
        completedAtIso: String,
        updatedAtIso: String
    ): DayTaskEntity {
        val target = entity.targetCount
        val isCountable = target != null && target > 0
        val isCompleted = entity.status == "COMPLETED"
        return when {
            isCompleted && isCountable -> entity.copy(
                status = "PENDING",
                currentCount = 0,
                completedAt = null,
                hasBeenRewarded = false,
                updatedAt = updatedAtIso
            )
            isCompleted -> entity.copy(
                status = "PENDING",
                completedAt = null,
                hasBeenRewarded = false,
                updatedAt = updatedAtIso
            )
            isCountable -> {
                val newCount = entity.currentCount + 1
                if (newCount >= target) {
                    entity.copy(
                        currentCount = newCount,
                        status = "COMPLETED",
                        completedAt = completedAtIso,
                        hasBeenRewarded = false,
                        updatedAt = updatedAtIso
                    )
                } else {
                    entity.copy(currentCount = newCount, updatedAt = updatedAtIso)
                }
            }
            else -> entity.copy(
                status = "COMPLETED",
                completedAt = completedAtIso,
                hasBeenRewarded = false,
                updatedAt = updatedAtIso
            )
        }
    }
}
