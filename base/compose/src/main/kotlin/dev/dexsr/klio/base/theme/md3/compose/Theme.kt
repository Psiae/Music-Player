package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import dev.dexsr.klio.base.theme.md3.MD3Theme

val MD3Theme.colorScheme: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColorScheme.current

val MD3Theme.lightColorScheme: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalLightColorScheme.current

val MD3Theme.darkColorScheme: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalDarkColorScheme.current

@Composable
fun MD3Theme.backgroundColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.background)
}

@Composable
fun MD3Theme.backgroundContentColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.onBackground)
}

@Composable
fun MD3Theme.primaryColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.primary)
}

@Composable
fun MD3Theme.surfaceColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.surface)
}

@Composable
fun MD3Theme.surfaceContentColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.onSurface)
}

@Composable
fun MD3Theme.surfaceVariantColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.surfaceVariant)
}

@Composable
fun MD3Theme.surfaceVariantContentColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.onSurfaceVariant)
}

@Composable
fun MD3Theme.secondaryContainerColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.secondaryContainer)
}

@Composable
fun MD3Theme.secondaryContainerContentColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.onSecondaryContainer)
}

@Composable
fun MD3Theme.surfaceColorAsState(
    transform: (Color) -> Color
): State<Color> {
    return rememberUpdatedState(newValue = transform(colorScheme.surface))
}

@Composable
fun MD3Theme.outlineVariantColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.outlineVariant)
}

@Composable
fun MD3Theme.surfaceTintColorAsState(): State<Color> {
    return rememberUpdatedState(newValue = colorScheme.surfaceTint)
}

@Composable
inline fun <T> MD3Theme.foldLightOrDarkTheme(
    light: () -> T,
    dark: () -> T
): T = foldLightOrDarkTheme(!LocalIsThemeDark.current, light, dark)

inline fun <T> MD3Theme.foldLightOrDarkTheme(
    isLight: Boolean,
    light: () -> T,
    dark: () -> T
): T = if (isLight) light() else dark()

@Composable
inline fun MD3Theme.blackOrWhite(): Color = if (LocalIsThemeDark.current) {
    Color.Black
} else {
    Color.White
}

@Composable
inline fun MD3Theme.blackOrWhiteContent(): Color = if (LocalIsThemeDark.current) {
    Color.White
} else {
    Color.Black
}
