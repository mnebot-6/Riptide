package com.mnebot.riptide.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Utility to refresh all Riptide widgets from within the app.
 * Call after task completion, creation, deletion, or day change.
 */
object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(RiptideWidget::class.java)
        glanceIds.forEach { glanceId ->
            RiptideWidget().update(context, glanceId)
        }
    }
}
