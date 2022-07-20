package com.kylentt.musicplayer.ui.main.compose.theme.color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

object ColorHelper {

	@Composable
	fun getTonedSurface(
		color: Color = MaterialTheme.colorScheme.primary,
		surface: Color = MaterialTheme.colorScheme.surface,
		elevation: Dp,
	): Color {
		val alpha = ((4.5f * ln( x = (elevation).value + 1) ) + 2f) / 100f
		return color.copy(alpha = alpha).compositeOver(surface)
	}
}
