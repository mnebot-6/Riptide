package com.mnebot.riptide.domain.model

data class DecorationProgress(
    val perfectDaysStreak: Int,       // días consecutivos actuales con score 1.0f
    val completedTasksTotal: Int,     // acumulado histórico de tareas COMPLETED
    val wallpaperActivated: Boolean,  // true si el live wallpaper fue activado alguna vez
    val googleSignedIn: Boolean = false,         // true si el usuario inició sesión con Google
    val longestPerfectStreak: Int = 0,           // racha perfecta más larga (para Coral Throne, 14)
    val hasCompletedAnyCategory: Boolean = false  // true si alguna categoría tiene todas sus especies
)
