package com.mnebot.riptide.widget

object WidgetTaskSorter {
    private const val NO_TIME = Int.MAX_VALUE

    fun sort(items: List<WidgetTaskItem>): List<WidgetTaskItem> {
        val comparator = compareBy<WidgetTaskItem>(
            { it.isCompleted },
            { it.taskTimeMinutes ?: NO_TIME },
            { it.blockTimeMinutes ?: NO_TIME },
            { it.title.lowercase() }
        )
        return items.sortedWith(comparator)
    }
}
