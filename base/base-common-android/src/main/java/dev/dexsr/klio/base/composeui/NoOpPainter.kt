package dev.dexsr.klio.base.composeui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.LayoutDirection

object NoOpPainter : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun applyAlpha(alpha: Float): Boolean = false

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean = false

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean = false

    override fun DrawScope.onDraw() {
        // NO-OP
    }
}
