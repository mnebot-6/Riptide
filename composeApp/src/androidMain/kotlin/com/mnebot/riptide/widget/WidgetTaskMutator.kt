package com.mnebot.riptide.widget

import com.mnebot.riptide.data.local.entity.DayTaskEntity

object WidgetTaskMutator {

    fun toggleInSnapshot(snapshot: WidgetSnapshot, taskId: String): WidgetSnapshot {
        val updatedItems = snapshot.items.map { item ->
            if (item.id != taskId) item else applyClick(item)
        }
        return snapshot.copy(items = WidgetTaskSorter.sort(updatedItems))
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

    fun applyClickToEntity(entity: DayTaskEntity, nowIso: String): DayTaskEntity {
        val target = entity.targetCount
        val isCountable = target != null && target > 0
        val isCompleted = entity.status == "COMPLETED"
        return when {
            isCompleted && isCountable -> entity.copy(
                status = "PENDING",
                currentCount = 0,
                completedAt = null,
                hasBeenRewarded = false,
                updatedAt = nowIso
            )
            isCompleted -> entity.copy(
                status = "PENDING",
                completedAt = null,
                hasBeenRewarded = false,
                updatedAt = nowIso
            )
            isCountable -> {
                val newCount = entity.currentCount + 1
                if (newCount >= target) {
                    entity.copy(
                        currentCount = newCount,
                        status = "COMPLETED",
                        completedAt = nowIso,
                        hasBeenRewarded = false,
                        updatedAt = nowIso
                    )
                } else {
                    entity.copy(currentCount = newCount, updatedAt = nowIso)
                }
            }
            else -> entity.copy(
                status = "COMPLETED",
                completedAt = nowIso,
                hasBeenRewarded = false,
                updatedAt = nowIso
            )
        }
    }
}
