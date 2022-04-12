package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.theme.defaults

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColors(
  primary = Purple200,
  primaryVariant = Purple700,
  secondary = Teal200
)

private val LightColorPalette = lightColors(
  primary = Purple500,
  primaryVariant = Purple700,
  secondary = Teal200

  /* Other default colors to override
  background = Color.White,
  surface = Color.White,
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = Color.Black,
  onSurface = Color.Black,
  */
)


@Composable
fun MediaPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val systemUiController = rememberSystemUiController()

  if (darkTheme) {
    systemUiController.setSystemBarsColor(color = Color.Transparent)
  } else systemUiController.setSystemBarsColor(color = Color.White)

  val colors = if (darkTheme) {
    DarkColorPalette
  } else {
    LightColorPalette
  }

  MaterialTheme(
    colors = colors,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}

private val lightColorScheme = lightColorScheme(
  primary = Purple500,

  )

private val LightColorPalette3 = lightColors(
  primary = Purple500,
  primaryVariant = Purple700,
  secondary = Teal200
)

private val DarkColorPalette3 = darkColors(
  primary = Purple200,
  primaryVariant = Purple700,
  secondary = Teal200
)

@Composable
fun MediaPlayerTheme3(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val systemUiController = rememberSystemUiController()

  if (darkTheme) {
    systemUiController.setSystemBarsColor(color = Color.Transparent)
  } else systemUiController.setSystemBarsColor(color = Color.White)

  val colors = if (darkTheme) {
    DarkColorPalette3
  } else {
    LightColorPalette3
  }

  MaterialTheme(
    colors = colors,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}
