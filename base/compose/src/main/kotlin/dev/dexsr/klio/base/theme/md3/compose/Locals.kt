package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.runtime.staticCompositionLocalOf

val LocalIsThemeDark = staticCompositionLocalOf<Boolean> { false }
val LocalDarkColorScheme = staticCompositionLocalOf { defaultDarkColorScheme() }
val LocalLightColorScheme = staticCompositionLocalOf { defaultLightColorScheme() }
val LocalColorScheme = staticCompositionLocalOf { defaultLightColorScheme() }
