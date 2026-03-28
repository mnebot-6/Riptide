package com.mnebot.riptide.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/**
 * Utility to refresh all Riptide widgets from within the app.
 * Call after task completion, creation, deletion, or day change.
 */
object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        RiptideWidget().updateAll(context)
    }
}
