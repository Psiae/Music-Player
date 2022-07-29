package com.kylentt.musicplayer.ui.main.compose.theme.color

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import kotlin.math.ln

object ColorHelper {

	fun getTonedSurface(
		color: Color,
		surfaceColor: Color,
		elevation: Dp,
	): Color {
		val alpha = ((4.5f * ln(x = (elevation).value + 1)) + 2f) / 100f
		return color.copy(alpha = alpha).compositeOver(surfaceColor)
	}

	@Composable
	fun tonePrimarySurface(
		color: Color = MaterialTheme.colorScheme.primary,
		surfaceColor: Color = MaterialTheme.colorScheme.surface,
		elevation: Dp
	) = getTonedSurface(color, surfaceColor, elevation)

	@Composable
	fun textColor(darkMode: Boolean = isSystemInDarkTheme()): Color {
		return if (darkMode) Color.White else Color.Black
	}

}
