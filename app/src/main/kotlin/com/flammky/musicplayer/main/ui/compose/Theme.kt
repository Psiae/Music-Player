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
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasSnowCone
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.color.DynamicColors

@Composable
fun MaterialDesign3Theme(
	dynamic: Boolean = AndroidAPI.hasSnowCone(),
	useDarkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	val lightColors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
		dynamicLightColorScheme(LocalContext.current)
	} else {
		Theme.defaultLightColorScheme()
	}
	val darkColors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
		dynamicDarkColorScheme(LocalContext.current)
	} else {
		Theme.defaultDarkColorScheme()
	}
	Theme.ProvideTheme(
		isDefaultDark = useDarkTheme,
		lightColorScheme = lightColors,
		darkColorScheme = darkColors
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
