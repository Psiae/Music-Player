package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasSnowCone
import com.google.android.material.color.DynamicColors
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun DefaultMaterial3Theme(
	dynamic: Boolean = AndroidAPI.hasSnowCone(),
	dark: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit,
) {
    val lightColors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        defaultLightColorScheme()
    }
    val darkColors = if (dynamic && DynamicColors.isDynamicColorAvailable()) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        defaultDarkColorScheme()
    }
    CompositionLocalProvider(
        LocalIsThemeDark provides dark,
        LocalDarkColorScheme provides darkColors,
        LocalLightColorScheme provides lightColors,
        LocalColorScheme provides if (dark) darkColors else lightColors,
    ) {
        Material3Theme(
            colorScheme = if (dark) darkColors else lightColors
        ) {
            content()
        }
    }
}
