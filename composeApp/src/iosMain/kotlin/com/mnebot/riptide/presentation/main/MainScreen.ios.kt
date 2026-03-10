package com.mnebot.riptide.presentation.main

import androidx.compose.ui.graphics.Color

actual fun parseColor(hex: String): Color {
    val hex = hex.trimStart('#')
    if (hex.length != 6) return Color(0xFF2E5F9E)
    val r = hex.substring(0, 2).toIntOrNull(16) ?: return Color(0xFF2E5F9E)
    val g = hex.substring(2, 4).toIntOrNull(16) ?: return Color(0xFF2E5F9E)
    val b = hex.substring(4, 6).toIntOrNull(16) ?: return Color(0xFF2E5F9E)
    return Color(r, g, b)
}