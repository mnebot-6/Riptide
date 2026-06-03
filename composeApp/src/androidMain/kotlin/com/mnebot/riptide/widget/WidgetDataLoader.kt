package com.mnebot.riptide.widget

import android.content.Context
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.local.entity.WorkBlockEntity
import com.mnebot.riptide.data.local.mapper.parseRecurrence
import com.mnebot.riptide.domain.model.Recurrence
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object WidgetDataLoader {

    suspend fun load(context: Context): WidgetSnapshot {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today: LocalDate = now.date
        val todayIso = today.toString()
        val isoDayOfWeek = today.dayOfWeek.isoDayNumber

        val db = DatabaseProvider.getDatabase(context)
        val tasks = db.dayTaskDao().getByDate(todayIso)
        val blocks = db.workBlockDao().getAll()
        val blocksById = blocks.associateBy { it.id }

        val blockTimeCache = HashMap<String, Int?>(blocks.size)

        val items = tasks
            .filter { it.status != "POSTPONED" && !it.isDeleted }
            .map { entity ->
                val block: WorkBlockEntity? = entity.blockId?.let { blocksById[it] }
                val blockTimeMinutes = block?.let {
                    blockTimeCache.getOrPut(it.id) { resolveBlockStartMinutes(it, isoDayOfWeek) }
                }
                val targetCount = entity.targetCount
                val isCountable = targetCount != null && targetCount > 0
                WidgetTaskItem(
                    id = entity.id,
                    title = entity.title,
                    blockColorHex = block?.color,
                    isCompleted = entity.status == "COMPLETED",
                    isCountable = isCountable,
                    currentCount = entity.currentCount,
                    targetCount = targetCount,
                    taskTimeMinutes = parseTimeToMinutes(entity.time),
                    blockTimeMinutes = blockTimeMinutes,
                    isPriority = entity.isPriority
                )
            }
            .let { WidgetTaskSorter.sort(it) }

        return WidgetSnapshot(
            items = items,
            dateIso = todayIso,
            generatedAtIso = now.toString()
        )
    }

    private fun resolveBlockStartMinutes(block: WorkBlockEntity, isoDayOfWeek: Int): Int? {
        val recurrence = parseRecurrence(block.recurrenceJson)
        val weekly = recurrence as? Recurrence.Weekly ?: return null
        val slot = weekly.slots.firstOrNull { it.dayOfWeek == isoDayOfWeek } ?: return null
        return slot.startTime?.toMinutesOfDay()
    }

    private fun parseTimeToMinutes(time: String?): Int? {
        if (time.isNullOrBlank()) return null
        val parts = time.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    private fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute
}
