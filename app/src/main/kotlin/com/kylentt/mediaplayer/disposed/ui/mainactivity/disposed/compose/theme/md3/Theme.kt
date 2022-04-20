package com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.theme.md3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kylentt.mediaplayer.helper.VersionHelper
import timber.log.Timber

private val LightThemeColors = lightColorScheme(

  primary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_primary,
  onPrimary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onPrimary,
  primaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_primaryContainer,
  onPrimaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onPrimaryContainer,
  secondary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_secondary,
  onSecondary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onSecondary,
  secondaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_secondaryContainer,
  onSecondaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onSecondaryContainer,
  tertiary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_tertiary,
  onTertiary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onTertiary,
  tertiaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_tertiaryContainer,
  onTertiaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onTertiaryContainer,
  error = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_error,
  errorContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_errorContainer,
  onError = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onError,
  onErrorContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onErrorContainer,
  background = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_background,
  onBackground = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onBackground,
  surface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_surface,
  onSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onSurface,
  surfaceVariant = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_surfaceVariant,
  onSurfaceVariant = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_onSurfaceVariant,
  outline = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_outline,
  inverseOnSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_inverseOnSurface,
  inverseSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_inverseSurface,
  inversePrimary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_light_inversePrimary,
)
private val DarkThemeColors = darkColorScheme(

  primary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_primary,
  onPrimary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onPrimary,
  primaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_primaryContainer,
  onPrimaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onPrimaryContainer,
  secondary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_secondary,
  onSecondary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onSecondary,
  secondaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_secondaryContainer,
  onSecondaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onSecondaryContainer,
  tertiary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_tertiary,
  onTertiary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onTertiary,
  tertiaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_tertiaryContainer,
  onTertiaryContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onTertiaryContainer,
  error = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_error,
  errorContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_errorContainer,
  onError = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onError,
  onErrorContainer = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onErrorContainer,
  background = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_background,
  onBackground = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onBackground,
  surface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_surface,
  onSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onSurface,
  surfaceVariant = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_surfaceVariant,
  onSurfaceVariant = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_onSurfaceVariant,
  outline = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_outline,
  inverseOnSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_inverseOnSurface,
  inverseSurface = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_inverseSurface,
  inversePrimary = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.md_theme_dark_inversePrimary,
)


@Composable
fun MaterialTheme3(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  Timber.d("ComposeDebug MaterialTheme3")

  val systemUiController = rememberSystemUiController()
  val color = if (VersionHelper.hasSnowCone()) {
    val context = LocalContext.current
    if (darkTheme) {
      dynamicDarkColorScheme(context)
    } else {
      dynamicLightColorScheme(context)
    }
  } else {
    if (darkTheme) {
      DarkThemeColors
    } else {
      LightThemeColors
    }
  }

  MaterialTheme(
    colorScheme = color,
    typography = com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.AppTypography,
  ) {
    Timber.d("ComposeDebug MaterialTheme Content")
    systemUiController.setStatusBarColor(ColorUtil.getTonedSurface().copy(alpha = 0.5f))
    systemUiController.setNavigationBarColor(Color.Transparent)
    systemUiController.isNavigationBarContrastEnforced = false
    systemUiController.statusBarDarkContentEnabled = !darkTheme
    systemUiController.navigationBarDarkContentEnabled = !darkTheme

    content()
  }
}
