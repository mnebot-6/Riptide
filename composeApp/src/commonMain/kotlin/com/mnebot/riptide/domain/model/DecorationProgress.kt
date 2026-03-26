package com.mnebot.riptide.domain.model

data class DecorationProgress(
    val perfectDaysStreak: Int,       // días consecutivos actuales con score 1.0f
    val completedTasksTotal: Int,     // acumulado histórico de tareas COMPLETED
    val wallpaperActivated: Boolean   // true si el live wallpaper fue activado alguna vez
)
