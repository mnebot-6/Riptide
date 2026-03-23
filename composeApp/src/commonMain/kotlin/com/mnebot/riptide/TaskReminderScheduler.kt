package com.mnebot.riptide

import kotlinx.datetime.LocalDateTime

interface TaskReminderScheduler {
    fun scheduleReminder(taskId: String, title: String, scheduledAt: LocalDateTime)
    fun cancelReminder(taskId: String)
    suspend fun rescheduleAll()
}
