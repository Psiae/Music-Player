package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.theme.md3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import kotlin.math.ln

object ColorUtil {

  @Composable
  fun getTonedSurface(el: Int = 2): Color {
    val alpha = ((4.5f * ln((el).dp.value + 1)) + 2f) / 100f
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    return primary.copy(alpha = alpha).compositeOver(surface)
  }
}

object DefaultColor {

  val black = Color(0xFF000000)
  val white = Color(0xFFFFFFFF)

  val lightText = Color(0xFFFFFFFF)
  val darkText = Color(0xFF0A0A0A)

  val darkThemeBackground = Color(0xFF0F0F0F)
  val lightThemeBackground = white

  val Icon1 = Color(0xFFC5E3F6)
  val Icon2 = Color(0xFFFC5C9C)

  @Composable
  fun getBlack20(): Color = Color.Black.copy(alpha = 0.20f)

  @Composable
  fun getDNBackground(): Color =
    if (isSystemInDarkTheme()) darkThemeBackground else lightThemeBackground

  @Composable
  fun getDNTextColor(): Color = if (isSystemInDarkTheme()) lightText else darkText

  @Composable
  fun getSurfaceIconTint(): Color = MaterialTheme.colorScheme.onSurface

}

val md_theme_light_primary = Color(0xFF006685)
val md_theme_light_onPrimary = Color(0xFFffffff)
val md_theme_light_primaryContainer = Color(0xFFbce9ff)
val md_theme_light_onPrimaryContainer = Color(0xFF001f2a)
val md_theme_light_secondary = Color(0xFF00668a)
val md_theme_light_onSecondary = Color(0xFFffffff)
val md_theme_light_secondaryContainer = Color(0xFFc0e8ff)
val md_theme_light_onSecondaryContainer = Color(0xFF001e2c)
val md_theme_light_tertiary = Color(0xFFb21e63)
val md_theme_light_onTertiary = Color(0xFFffffff)
val md_theme_light_tertiaryContainer = Color(0xFFffd9e4)
val md_theme_light_onTertiaryContainer = Color(0xFF3e001c)
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)
val md_theme_light_background = Color(0xFFfbfdfd)
val md_theme_light_onBackground = Color(0xFF191919)
val md_theme_light_surface = Color(0xFFfbfdfd)
val md_theme_light_onSurface = Color(0xFF191c1d)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_outline = Color(0xFF79747E)
val md_theme_light_inverseOnSurface = Color(0xFFeff1f1)
val md_theme_light_inverseSurface = Color(0xFF2d3132)
val md_theme_light_inversePrimary = Color(0xFF65d3ff)
val md_theme_light_shadow = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFF65d3ff)
val md_theme_dark_onPrimary = Color(0xFF003547)
val md_theme_dark_primaryContainer = Color(0xFF004d66)
val md_theme_dark_onPrimaryContainer = Color(0xFFbce9ff)
val md_theme_dark_secondary = Color(0xFF71d1ff)
val md_theme_dark_onSecondary = Color(0xFF003549)
val md_theme_dark_secondaryContainer = Color(0xFF004c68)
val md_theme_dark_onSecondaryContainer = Color(0xFFc0e8ff)
val md_theme_dark_tertiary = Color(0xFFffb0ca)
val md_theme_dark_onTertiary = Color(0xFF650032)
val md_theme_dark_tertiaryContainer = Color(0xFF8e0049)
val md_theme_dark_onTertiaryContainer = Color(0xFFffd9e4)
val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)
val md_theme_dark_background = Color(0xFF191919)
val md_theme_dark_onBackground = Color(0xFFe0e3e3)
val md_theme_dark_surface = Color(0xFF1B1C1D)
val md_theme_dark_onSurface = Color(0xFFe0e3e3)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)
val md_theme_dark_outline = Color(0xFF938F99)
val md_theme_dark_inverseOnSurface = Color(0xFF191c1d)
val md_theme_dark_inverseSurface = Color(0xFFe0e3e3)
val md_theme_dark_inversePrimary = Color(0xFF006685)
val md_theme_dark_shadow = Color(0xFF000000)


val seed = Color(0xFF6750A4)
val error = Color(0xFFB3261E)
