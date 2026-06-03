package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Refresh all Riptide widgets from within the app. Re-reads the snapshot from Room and
 * pushes it into the Glance state for each widget instance, then requests a recomposition.
 */
object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    suspend fun refreshAll(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(RiptideWidget::class.java)
            if (glanceIds.isEmpty()) return
            val snapshot = WidgetDataLoader.load(context)
            val widget = RiptideWidget()
            glanceIds.forEach { id ->
                updateAppWidgetState(context, WidgetSnapshotStateDefinition, id) { snapshot }
                widget.update(context, id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh widgets", e)
        }
    }
}
