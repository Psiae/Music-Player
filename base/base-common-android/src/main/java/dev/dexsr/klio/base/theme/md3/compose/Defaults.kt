package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object DefaultToken {
    val md_theme_light_primary = Color(0xFF0C61A4)
    val md_theme_light_onPrimary = Color(0xFFFFFFFF)
    val md_theme_light_primaryContainer = Color(0xFFD2E4FF)
    val md_theme_light_onPrimaryContainer = Color(0xFF001C37)
    val md_theme_light_secondary = Color(0xFF0061A3)
    val md_theme_light_onSecondary = Color(0xFFFFFFFF)
    val md_theme_light_secondaryContainer = Color(0xFFD1E4FF)
    val md_theme_light_onSecondaryContainer = Color(0xFF001D36)
    val md_theme_light_tertiary = Color(0xFF794A99)
    val md_theme_light_onTertiary = Color(0xFFFFFFFF)
    val md_theme_light_tertiaryContainer = Color(0xFFF4DAFF)
    val md_theme_light_onTertiaryContainer = Color(0xFF2F004C)
    val md_theme_light_error = Color(0xFFBA1A1A)
    val md_theme_light_errorContainer = Color(0xFFFFDAD6)
    val md_theme_light_onError = Color(0xFFFFFFFF)
    val md_theme_light_onErrorContainer = Color(0xFF410002)
    val md_theme_light_background = Color(0xFFFDFCFF)
    val md_theme_light_onBackground = Color(0xFF1A1C1E)
    val md_theme_light_surface = Color(0xFFFDFCFF)
    val md_theme_light_onSurface = Color(0xFF1A1C1E)
    val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
    val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
    val md_theme_light_outline = Color(0xFF73777F)
    val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
    val md_theme_light_inverseSurface = Color(0xFF2F3033)
    val md_theme_light_inversePrimary = Color(0xFFA0C9FF)
    val md_theme_light_shadow = Color(0xFF000000)
    val md_theme_light_surfaceTint = Color(0xFF0C61A4)
    val md_theme_light_outlineVariant = Color(0xFFC3C6CF)
    val md_theme_light_scrim = Color(0xFF000000)

    val md_theme_dark_primary = Color(0xFFA0C9FF)
    val md_theme_dark_onPrimary = Color(0xFF00325A)
    val md_theme_dark_primaryContainer = Color(0xFF00497F)
    val md_theme_dark_onPrimaryContainer = Color(0xFFD2E4FF)
    val md_theme_dark_secondary = Color(0xFF9ECAFF)
    val md_theme_dark_onSecondary = Color(0xFF003258)
    val md_theme_dark_secondaryContainer = Color(0xFF00497C)
    val md_theme_dark_onSecondaryContainer = Color(0xFFD1E4FF)
    val md_theme_dark_tertiary = Color(0xFFE3B5FF)
    val md_theme_dark_onTertiary = Color(0xFF481867)
    val md_theme_dark_tertiaryContainer = Color(0xFF60317F)
    val md_theme_dark_onTertiaryContainer = Color(0xFFF4DAFF)
    val md_theme_dark_error = Color(0xFFFFB4AB)
    val md_theme_dark_errorContainer = Color(0xFF93000A)
    val md_theme_dark_onError = Color(0xFF690005)
    val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
    val md_theme_dark_background = Color(0xFF1A1C1E)
    val md_theme_dark_onBackground = Color(0xFFE3E2E6)
    val md_theme_dark_surface = Color(0xFF1A1C1E)
    val md_theme_dark_onSurface = Color(0xFFE3E2E6)
    val md_theme_dark_surfaceVariant = Color(0xFF43474E)
    val md_theme_dark_onSurfaceVariant = Color(0xFFC3C6CF)
    val md_theme_dark_outline = Color(0xFF8D9199)
    val md_theme_dark_inverseOnSurface = Color(0xFF1A1C1E)
    val md_theme_dark_inverseSurface = Color(0xFFE3E2E6)
    val md_theme_dark_inversePrimary = Color(0xFF0C61A4)
    val md_theme_dark_shadow = Color(0xFF000000)
    val md_theme_dark_surfaceTint = Color(0xFFA0C9FF)
    val md_theme_dark_outlineVariant = Color(0xFF43474E)
    val md_theme_dark_scrim = Color(0xFF000000)

    val seed = Color(0xFF0E61A4)
    val CustomColor1 = Color(0xFF0B2036)
    val light_CustomColor1 = Color(0xFF0D61A4)
    val light_onCustomColor1 = Color(0xFFFFFFFF)
    val light_CustomColor1Container = Color(0xFFD2E4FF)
    val light_onCustomColor1Container = Color(0xFF001C37)
    val dark_CustomColor1 = Color(0xFFA0C9FF)
    val dark_onCustomColor1 = Color(0xFF00325A)
    val dark_CustomColor1Container = Color(0xFF00497F)
    val dark_onCustomColor1Container = Color(0xFFD2E4FF)
}

fun defaultLightColorScheme(): ColorScheme = lightColorScheme(
    primary = DefaultToken.md_theme_light_primary,
    onPrimary = DefaultToken.md_theme_light_onPrimary,
    primaryContainer = DefaultToken.md_theme_light_primaryContainer,
    onPrimaryContainer = DefaultToken.md_theme_light_onPrimaryContainer,
    secondary = DefaultToken.md_theme_light_secondary,
    onSecondary = DefaultToken.md_theme_light_onSecondary,
    secondaryContainer = DefaultToken.md_theme_light_secondaryContainer,
    onSecondaryContainer = DefaultToken.md_theme_light_onSecondaryContainer,
    tertiary = DefaultToken.md_theme_light_tertiary,
    onTertiary = DefaultToken.md_theme_light_onTertiary,
    tertiaryContainer = DefaultToken.md_theme_light_tertiaryContainer,
    onTertiaryContainer = DefaultToken.md_theme_light_onTertiaryContainer,
    error = DefaultToken.md_theme_light_error,
    errorContainer = DefaultToken.md_theme_light_errorContainer,
    onError = DefaultToken.md_theme_light_onError,
    onErrorContainer = DefaultToken.md_theme_light_onErrorContainer,
    background = DefaultToken.md_theme_light_background,
    onBackground = DefaultToken.md_theme_light_onBackground,
    surface = DefaultToken.md_theme_light_surface,
    onSurface = DefaultToken.md_theme_light_onSurface,
    surfaceVariant = DefaultToken.md_theme_light_surfaceVariant,
    onSurfaceVariant = DefaultToken.md_theme_light_onSurfaceVariant,
    outline = DefaultToken.md_theme_light_outline,
    inverseOnSurface = DefaultToken.md_theme_light_inverseOnSurface,
    inverseSurface = DefaultToken.md_theme_light_inverseSurface,
    inversePrimary = DefaultToken.md_theme_light_inversePrimary,
    surfaceTint = DefaultToken.md_theme_light_surfaceTint,
)

fun defaultDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = DefaultToken.md_theme_dark_primary,
    onPrimary = DefaultToken.md_theme_dark_onPrimary,
    primaryContainer = DefaultToken.md_theme_dark_primaryContainer,
    onPrimaryContainer = DefaultToken.md_theme_dark_onPrimaryContainer,
    secondary = DefaultToken.md_theme_dark_secondary,
    onSecondary = DefaultToken.md_theme_dark_onSecondary,
    secondaryContainer = DefaultToken.md_theme_dark_secondaryContainer,
    onSecondaryContainer = DefaultToken.md_theme_dark_onSecondaryContainer,
    tertiary = DefaultToken.md_theme_dark_tertiary,
    onTertiary = DefaultToken.md_theme_dark_onTertiary,
    tertiaryContainer = DefaultToken.md_theme_dark_tertiaryContainer,
    onTertiaryContainer = DefaultToken.md_theme_dark_onTertiaryContainer,
    error = DefaultToken.md_theme_dark_error,
    errorContainer = DefaultToken.md_theme_dark_errorContainer,
    onError = DefaultToken.md_theme_dark_onError,
    onErrorContainer = DefaultToken.md_theme_dark_onErrorContainer,
    background = DefaultToken.md_theme_dark_background,
    onBackground = DefaultToken.md_theme_dark_onBackground,
    surface = DefaultToken.md_theme_dark_surface,
    onSurface = DefaultToken.md_theme_dark_onSurface,
    surfaceVariant = DefaultToken.md_theme_dark_surfaceVariant,
    onSurfaceVariant = DefaultToken.md_theme_dark_onSurfaceVariant,
    outline = DefaultToken.md_theme_dark_outline,
    inverseOnSurface = DefaultToken.md_theme_dark_inverseOnSurface,
    inverseSurface = DefaultToken.md_theme_dark_inverseSurface,
    inversePrimary = DefaultToken.md_theme_dark_inversePrimary,
    surfaceTint = DefaultToken.md_theme_dark_surfaceTint,
)
