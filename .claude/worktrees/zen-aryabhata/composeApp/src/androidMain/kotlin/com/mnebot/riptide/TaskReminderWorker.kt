package com.mnebot.riptide

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        NotificationHelper.sendTaskReminderNotification(context, taskTitle, taskId)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID    = "task_id"
        const val KEY_TASK_TITLE = "task_title"
    }
}
