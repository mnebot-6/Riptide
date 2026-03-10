package com.mnebot.riptide.presentation.main

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

actual fun parseColor(hex: String): Color {
    return try {
        Color(hex.toColorInt())
    } catch (e: Exception) {
        Color(0xFF2E5F9E)
    }
}