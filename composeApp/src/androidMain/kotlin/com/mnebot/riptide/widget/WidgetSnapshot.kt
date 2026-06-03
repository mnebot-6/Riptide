package com.mnebot.riptide.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetTaskItem(
    val id: String,
    val title: String,
    val blockColorHex: String?,
    val isCompleted: Boolean,
    val isCountable: Boolean,
    val currentCount: Int,
    val targetCount: Int?,
    val taskTimeMinutes: Int?,
    val blockTimeMinutes: Int?,
    val isPriority: Boolean
)

@Serializable
data class WidgetSnapshot(
    val items: List<WidgetTaskItem>,
    val dateIso: String,
    val generatedAtIso: String
) {
    companion object {
        val EMPTY = WidgetSnapshot(items = emptyList(), dateIso = "", generatedAtIso = "")
    }
}
