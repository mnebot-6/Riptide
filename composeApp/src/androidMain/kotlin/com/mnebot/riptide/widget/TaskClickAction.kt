package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.mnebot.riptide.data.local.db.DatabaseProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class TaskClickAction : ActionCallback {

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("widget_task_id")
        private const val TAG = "TaskClickAction"
        private val mutex = Mutex()
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        mutex.withLock {
            // 1) Optimistic mutation of the snapshot (synchronous, before any DB I/O).
            //    This makes the click feel instant: Glance recomposes from updated state.
            val widget = RiptideWidget()
            updateAppWidgetState(context, WidgetSnapshotStateDefinition, glanceId) { snap ->
                WidgetTaskMutator.toggleInSnapshot(snap, taskId)
            }
            widget.update(context, glanceId)

            // 2) Persist to Room asynchronously (within this coroutine, but after UI is updated).
            try {
                val db = DatabaseProvider.getDatabase(context)
                val dao = db.dayTaskDao()
                val entity = dao.getById(taskId)
                if (entity != null) {
                    val nowIso = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString()
                    val updated = WidgetTaskMutator.applyClickToEntity(entity, nowIso)
                    dao.update(updated)
                }

                // 3) Reconcile from DB across all widget instances.
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(RiptideWidget::class.java)
                val fresh = WidgetDataLoader.load(context)
                ids.forEach { gid ->
                    updateAppWidgetState(context, WidgetSnapshotStateDefinition, gid) { fresh }
                    widget.update(context, gid)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting click for $taskId", e)
            }
        }
    }
}
