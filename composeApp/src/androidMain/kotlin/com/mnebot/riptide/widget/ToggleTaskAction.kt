package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.mnebot.riptide.data.local.db.DatabaseProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ToggleTaskAction : ActionCallback {

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id")
        private const val TAG = "ToggleTaskAction"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return

        try {
            val db = DatabaseProvider.getDatabase(context)
            val dao = db.dayTaskDao()
            val task = dao.getById(taskId) ?: return

            if (task.status == "COMPLETED") {
                // Uncomplete: check if night summary exists for this date
                val summaryExists = task.date?.let {
                    db.daySummaryDao().getByDate(it) != null
                } ?: false
                val newStatus = if (summaryExists) "EXPIRED" else "PENDING"
                dao.update(task.copy(status = newStatus, completedAt = null))
            } else {
                // Complete: set status, timestamp, and reward flag
                val now = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                dao.update(
                    task.copy(
                        status = "COMPLETED",
                        completedAt = now.toString(),
                        hasBeenRewarded = true
                    )
                )
            }

            // Refresh all Riptide widgets
            RiptideWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling task $taskId", e)
        }
    }
}
