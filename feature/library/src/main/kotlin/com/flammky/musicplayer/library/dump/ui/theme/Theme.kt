package com.flammky.musicplayer.library.dump.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceColorAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState

internal object Theme : com.flammky.musicplayer.base.theme.Theme() {

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
		val svar = Theme.surfaceVariantColorAsState().value
		val s = Theme.surfaceColorAsState().value
		return remember(svar, s) {
			s.copy(alpha = 0.45f).compositeOver(svar)
		}
	}

	@Composable
	fun localShimmerColor(): Color {
		val sf = localShimmerSurface()
		val content = Theme.backgroundContentColorAsState().value
		return remember(sf, content) {
			content.copy(alpha = 0.6f).compositeOver(sf)
		}
	}
}
