package com.flammky.musicplayer.main.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.ProvideTheme
import com.flammky.musicplayer.base.theme.compose.defaultDarkColorScheme
import com.flammky.musicplayer.base.theme.compose.defaultLightColorScheme
import com.flammky.musicplayer.base.theme.compose.elevatedTonalPrimarySurfaceAsState
import com.flammky.musicplayer.core.build.AndroidAPI
import com.flammky.musicplayer.core.build.AndroidBuildVersion.hasSnowCone
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.color.DynamicColors

@Composable
fun MaterialDesign3Theme(
	dynamic: Boolean = AndroidAPI.hasSnowCone(),
	useDarkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val colors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
		if (useDarkTheme) {
			dynamicDarkColorScheme(LocalContext.current)
		} else {
			dynamicLightColorScheme(LocalContext.current)
		}
	} else {
		if (useDarkTheme) {
			Theme.defaultDarkColorScheme()
		} else {
			Theme.defaultLightColorScheme()
		}
	}
	Theme.ProvideTheme(
		isThemeDark = useDarkTheme,
		colorScheme = colors
	) {
		with(rememberSystemUiController()) {
			setStatusBarColor(Theme.elevatedTonalPrimarySurfaceAsState(elevation = 2.dp).value.copy(0.4f))
			setNavigationBarColor(Color.Transparent)
			isNavigationBarContrastEnforced = true
			statusBarDarkContentEnabled = !useDarkTheme
			navigationBarDarkContentEnabled = !useDarkTheme
		}
		content()
	}
}
