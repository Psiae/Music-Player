package com.flammky.musicplayer.ui.main.compose.theme

import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.core.sdk.VersionHelper
import com.flammky.musicplayer.ui.main.compose.theme.color.ColorHelper
import com.flammky.musicplayer.ui.main.compose.theme.text.MainTypography
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.color.DynamicColors

@Composable
fun MainMaterial3Theme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	dynamic: Boolean = VersionHelper.hasSnowCone(),
	content: @Composable () -> Unit
) {
	MaterialTheme(
		colorScheme = mainColorScheme(darkTheme = darkTheme, dynamic = dynamic),
		typography = MainTypography
	) {
		with(rememberSystemUiController()) {

			setNavigationBarColor(
				color = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f),
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

@Composable
private fun mainColorScheme(darkTheme: Boolean, dynamic: Boolean): ColorScheme {
	return if (dynamic && DynamicColors.isDynamicColorAvailable()) {
		dynamicColorScheme(darkTheme = darkTheme)
	} else {
		defaultColorScheme(darkTheme = darkTheme)
	}
}

@Composable
@RequiresApi(31)
private fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
	val context = LocalContext.current
	return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
private fun defaultColorScheme(darkTheme: Boolean): ColorScheme {
	return if (darkTheme) DarkThemeColors else LightThemeColors
}


