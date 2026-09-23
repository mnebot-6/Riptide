package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.local.nowIso
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
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            updateAppWidgetState(context, WidgetSnapshotStateDefinition, glanceId) { snap ->
                WidgetTaskMutator.toggleInSnapshot(snap, taskId, now.toString())
            }
            widget.update(context, glanceId)

            // 2) Persist to Room asynchronously (within this coroutine, but after UI is updated).
            try {
                val db = DatabaseProvider.getDatabase(context)
                val dao = db.dayTaskDao()
                val entity = dao.getById(taskId)
                // Solo el día en curso admite cambios de estado: si el widget arrastra
                // un snapshot de ayer, el clic no debe tocar un día ya cerrado.
                if (entity != null && entity.date == now.date.toString()) {
                    val updated = WidgetTaskMutator.applyClickToEntity(
                        entity,
                        completedAtIso = now.toString(),
                        updatedAtIso = nowIso()
                    )
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
