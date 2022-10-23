package com.flammky.musicplayer.library.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

internal object Theme {

	@Composable
	fun localSurfaceColor(): Color {
		val darkTheme = isSystemInDarkTheme()
		return remember(darkTheme) {
			if (darkTheme)
				Color(red = 25, green = 28, blue = 30)
			else
				Color(red = 245, green = 245, blue = 245)
		}
	}

	@Composable
	fun localImageSurfaceColor(): Color {
		val darkTheme = isSystemInDarkTheme()
		return remember(darkTheme) {
			if (darkTheme)
				Color(red = 40, green = 40, blue = 40)
			else
				Color(red = 200, green = 200, blue = 200)
		}
	}

	//
	// Should We Make Composition Local ?
	//

	@Composable
	fun localShimmerSurface(): Color {
		val darkTheme = isSystemInDarkTheme()
		return remember(darkTheme) {
			if (darkTheme) Color(0xFF323232) else Color(0xFFE6E6E6)
		}
	}

	@Composable
	fun localShimmerColor(): Color {
		val darkTheme = isSystemInDarkTheme()
		return remember(darkTheme) {
			if (darkTheme) Color(0xFF414141) else Color(0xFFEBEBEB)
		}
	}
}
