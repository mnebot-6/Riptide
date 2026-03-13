package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import platform.UIKit.NSStringDrawingUsesLineFragmentOrigin
import platform.UIKit.drawInRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsGetCurrentContext

actual fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean
) {
    val context = UIGraphicsGetCurrentContext() ?: return
    val fontSize = sizeSp.sp.toPx()
    val font = UIFont.systemFontOfSize(fontSize.toDouble())

    if (mirrored) {
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