package com.mnebot.riptide

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.local.mapper.toDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class TaskReminderSchedulerImpl(private val context: Context) : TaskReminderScheduler {

    override fun scheduleReminder(taskId: String, title: String, scheduledAt: LocalDateTime) {
        val now = Clock.System.now()
        val targetInstant = scheduledAt.toInstant(TimeZone.currentSystemDefault())
        val delayMillis = (targetInstant - now).inWholeMilliseconds
        if (delayMillis <= 0) return

        val data = workDataOf(
            TaskReminderWorker.KEY_TASK_ID    to taskId,
            TaskReminderWorker.KEY_TASK_TITLE to title
        )
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancelReminder(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(taskId))
    }

    override suspend fun rescheduleAll() {
        val db = DatabaseProvider.getDatabase(context)
        val tasks = db.dayTaskDao().getPendingWithNotifications()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        tasks.forEach { entity ->
            val task = entity.toDomain()
            val date = (task.schedule as? com.mnebot.riptide.domain.model.TaskSchedule.OneTime)?.date
                ?: return@forEach
            val time = task.schedule.time ?: return@forEach
            val scheduledAt = LocalDateTime(date, time)
            if (scheduledAt > now) {
                scheduleReminder(task.id, task.title, scheduledAt)
            }
        }
    }

    private fun workName(taskId: String) = "task_reminder_$taskId"
}
