package com.kylentt.musicplayer.ui.main.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.color.DynamicColors
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper

@Composable
inline fun Material3Theme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	dynamic: Boolean = DynamicColors.isDynamicColorAvailable(),
	crossinline content: @Composable () -> Unit
) {
	val context = LocalContext.current

	val colorScheme = when {
		dynamic && DynamicColors.isDynamicColorAvailable() -> {
			if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}
		else -> {
			if (darkTheme) DarkThemeColors else LightThemeColors
		}
	}

	MaterialTheme(colorScheme = colorScheme) {
		with(rememberSystemUiController()) {
			setNavigationBarColor(
				color = Color.Transparent,
				navigationBarContrastEnforced = true,
				darkIcons = !darkTheme
			)

			setStatusBarColor(
				color = ColorHelper.tonePrimarySurface(elevation = 2.dp).copy(alpha = 0.50f),
				darkIcons = !darkTheme
			)
		}
		content()
	}
}


