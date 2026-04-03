package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.mnebot.riptide.data.local.db.DatabaseProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ToggleTaskAction : ActionCallback {

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id")
        private const val TAG = "ToggleTaskAction"
        private val mutex = Mutex()
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return

        mutex.withLock {
            try {
                val db = DatabaseProvider.getDatabase(context)
                val dao = db.dayTaskDao()
                val task = dao.getById(taskId) ?: return

                // Already completed → do nothing
                if (task.status == "COMPLETED") return

                val now = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())

                if (task.targetCount != null && task.targetCount > 0) {
                    // Countable task: increment currentCount
                    val newCount = task.currentCount + 1
                    if (newCount >= task.targetCount) {
                        // Reached target → auto-complete
                        dao.update(
                            task.copy(
                                currentCount = newCount,
                                status = "COMPLETED",
                                completedAt = now.toString(),
                                hasBeenRewarded = false
                            )
                        )
                    } else {
                        // Just increment, stay in current status
                        dao.update(task.copy(currentCount = newCount))
                    }
                } else {
                    // Non-countable task: complete immediately.
                    // hasBeenRewarded stays false — the app will award XP when it opens next.
                    dao.update(
                        task.copy(
                            status = "COMPLETED",
                            completedAt = now.toString(),
                            hasBeenRewarded = false
                        )
                    )
                }

                // Refresh all Riptide widgets
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RiptideWidget::class.java)
                glanceIds.forEach { glanceId ->
                    RiptideWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling task $taskId", e)
            }
        }
    }
}
