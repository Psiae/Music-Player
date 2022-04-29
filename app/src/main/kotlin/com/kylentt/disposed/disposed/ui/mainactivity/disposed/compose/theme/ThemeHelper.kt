package com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import com.google.android.material.color.DynamicColors
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.theme.md3.Md3Defaults

object ThemeHelper {

  @Composable
  fun Context.getDynamicM3Color(darkTheme: Boolean): ColorScheme {
    val hasDynamic = DynamicColors.isDynamicColorAvailable()
    return if (hasDynamic) {
      if (darkTheme) {
        dynamicDarkColorScheme(this)
      } else {
        dynamicLightColorScheme(this)
      }
    } else {
      if (darkTheme) {
        Md3Defaults.defaultDarkThemeColors()
      } else {
        Md3Defaults.defaultLightThemeColors()
      }
    }
  }

}
