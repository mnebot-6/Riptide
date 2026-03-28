package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp

actual fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean,
    rotation: Float
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = sizeSp.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val fm = paint.fontMetrics
        val drawY = y - (fm.ascent + fm.descent) / 2f
        val native = canvas.nativeCanvas
        native.save()
        if (rotation != 0f) {
            // Emojis rotados: la dirección se controla negando la rotación,
            // no con mirror (que los dejaría panza arriba).
            val effectiveRotation = if (mirrored) -rotation else rotation
            native.rotate(effectiveRotation, x, y)
        } else if (mirrored) {
            native.scale(-1f, 1f, x, y)
        }
        native.drawText(emoji, x, drawY, paint)
        native.restore()
    }
}
