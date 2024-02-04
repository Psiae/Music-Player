package dev.dexsr.klio.base.composeui.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import dev.dexsr.klio.base.geometry.Vector2D
import kotlin.math.roundToInt

operator fun Offset.plus(vector2D: Vector2D): Offset {
	return Offset(
		x = x + vector2D.x,
		y = y + vector2D.y
	)
}

operator fun Offset.minus(vector2D: Vector2D): Offset {
	return Offset(
		x = x - vector2D.x,
		y = y - vector2D.y
	)
}

operator fun IntOffset.plus(vector2D: Vector2D): IntOffset {
	return IntOffset(x = x + vector2D.x.roundToInt(), y = y + vector2D.y.roundToInt())
}

fun Offset.roundToIntOffset(): IntOffset {
	return IntOffset(x = x.roundToInt(), y = y.roundToInt())
}
