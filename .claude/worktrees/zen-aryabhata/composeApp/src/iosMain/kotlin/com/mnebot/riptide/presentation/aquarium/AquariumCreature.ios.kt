package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import platform.UIKit.drawInRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGContextRotateCTM
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsGetCurrentContext

actual fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean,
    rotation: Float
) {
    val context = UIGraphicsGetCurrentContext() ?: return
    val fontSize = sizeSp.sp.toPx()
    val font = UIFont.systemFontOfSize(fontSize.toDouble())

    if (rotation != 0f) {
        val effectiveRotation = if (mirrored) -rotation else rotation
        CGContextTranslateCTM(context, x.toDouble(), y.toDouble())
        CGContextRotateCTM(context, (effectiveRotation * kotlin.math.PI / 180.0))
        CGContextTranslateCTM(context, -x.toDouble(), -y.toDouble())
    } else if (mirrored) {
        CGContextTranslateCTM(context, x.toDouble(), y.toDouble())
        CGContextScaleCTM(context, -1.0, 1.0)
        CGContextTranslateCTM(context, -x.toDouble(), -y.toDouble())
    }

    val attrs = mapOf(
        platform.UIKit.NSFontAttributeName to font
    )
    (emoji as platform.Foundation.NSString).drawInRect(
        CGRectMake(x.toDouble(), y.toDouble(), fontSize.toDouble() * 2, fontSize.toDouble() * 2),
        withAttributes = attrs
    )
}
