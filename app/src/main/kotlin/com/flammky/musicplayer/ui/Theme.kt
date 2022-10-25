package com.flammky.musicplayer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object Theme {

	@Composable
	fun backgroundColor(): Color {
		return MaterialTheme.colorScheme.background
	}

	@Composable
	fun dayNightAbsoluteColor(): Color {
		return if (isSystemInDarkTheme()) Color.Black else Color.White
	}

	@Composable
	fun dayNightAbsoluteContentColor(): Color {
		return if (isSystemInDarkTheme()) Color.White else Color.Black
	}
}
