package dev.dexsr.klio.base.composeui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import kotlin.math.ln
import kotlin.math.nextUp

inline fun Color.ifUnspecified(
    color: () -> Color
): Color = if (isUnspecified) color() else this

fun Color.nearestBlackOrWhite(
    whiteLuminanceThreshold: Float = 0.5f.nextUp()
) = if (luminance() >= whiteLuminanceThreshold) Color.White else Color.Black

fun Color.tintElevatedSurface(
    tint: Color,
    elevation: Dp,
): Color {
    val alpha = ((4.5f * ln(x = elevation.value + 1)) + 2f) / 100f
    return tint.copy(alpha = alpha).compositeOver(this)
}
