package com.mnebot.riptide

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    // ── Channel IDs ──────────────────────────────────────────────────────────
    const val CHANNEL_NIGHT_SUMMARY  = "night_summary"
    const val CHANNEL_MORNING_REMINDER = "morning_reminder"
    const val CHANNEL_TASK_REMINDER  = "task_reminder"

    // ── Notification IDs ─────────────────────────────────────────────────────
    private const val NOTIF_NIGHT_SUMMARY  = 1001
    private const val NOTIF_MORNING_REMINDER = 1002
    // Task reminders use task-hashCode as ID to allow one per task

    // ── Channel creation ─────────────────────────────────────────────────────

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NIGHT_SUMMARY,
                context.getString(R.string.channel_night_summary),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_night_summary_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MORNING_REMINDER,
                context.getString(R.string.channel_morning_reminder),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_morning_reminder_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASK_REMINDER,
                context.getString(R.string.channel_task_reminder),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_task_reminder_desc)
            }
        )
    }

    // ── Send helpers ─────────────────────────────────────────────────────────

    fun sendNightSummaryNotification(context: Context, completedCount: Int, totalCount: Int) {
        val notification = buildNotification(context, CHANNEL_NIGHT_SUMMARY) {
            setContentTitle(context.getString(R.string.notif_night_summary_title))
            setContentText(
                context.getString(R.string.notif_night_summary_body, completedCount, totalCount)
            )
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        NotificationManagerCompat.from(context).notify(NOTIF_NIGHT_SUMMARY, notification)
    }

    fun sendMorningReminderNotification(context: Context, untimedTaskTitles: List<String> = emptyList()) {
        val title = context.getString(R.string.notif_morning_reminder_title)
        val body = if (untimedTaskTitles.isEmpty()) {
            context.getString(R.string.notif_morning_reminder_body)
        } else {
            untimedTaskTitles.joinToString(separator = "\n") { "• $it" }
        }
        val notification = buildNotification(context, CHANNEL_MORNING_REMINDER) {
            setContentTitle(title)
            setContentText(body)
            if (untimedTaskTitles.isNotEmpty()) {
                setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        NotificationManagerCompat.from(context).notify(NOTIF_MORNING_REMINDER, notification)
    }

    fun sendTaskReminderNotification(context: Context, taskTitle: String, taskId: String) {
        val notification = buildNotification(context, CHANNEL_TASK_REMINDER) {
            setContentTitle(taskTitle)
            setContentText(context.getString(R.string.notif_task_reminder_body))
            setPriority(NotificationCompat.PRIORITY_HIGH)
        }
        // Use a stable numeric ID from the task ID string
        val notifId = taskId.hashCode()
        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    // ── Private builder ───────────────────────────────────────────────────────

    private fun buildNotification(
        context: Context,
        channelId: String,
        configure: NotificationCompat.Builder.() -> Unit
    ): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            channelId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply(configure)
            .build()
    }
}
