package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.palette.graphics.Palette
import com.flammky.musicplayer.player.presentation.root.runRemember
import kotlinx.coroutines.flow.Flow

data class MainScreenBackgroundDataSource(
    val observeCurrentPlaybackPalette: () -> Flow<Palette?>
)

data class MainScreenThemeInfo(
    val dark: Boolean,
    val backgroundColor: Color,
    val absBackgroundColor: Color
)

@Composable
fun MainScreenBackground(
    modifier: Modifier = Modifier,
    dataSource: MainScreenBackgroundDataSource,
    themeInfo: MainScreenThemeInfo
) = ArtworkThemedPlaybackBackground(modifier, dataSource, themeInfo)

@Composable
private fun ArtworkThemedPlaybackBackground(
    modifier: Modifier,
    dataSource: MainScreenBackgroundDataSource,
    themeInfo: MainScreenThemeInfo
) {
    val palette = remember(dataSource) {
        dataSource.observeCurrentPlaybackPalette()
    }.collectAsState(initial = null).value
    val backgroundColor = themeInfo.backgroundColor
    val absBackgroundColor = themeInfo.absBackgroundColor
    val darkTheme = themeInfo.dark
    val compositeBase =
        if (darkTheme) {
            backgroundColor
        } else {
            absBackgroundColor
        }
    val color = palette?.runRemember {
        if (darkTheme) {
            getDarkVibrantColor(getMutedColor(getDominantColor(-1)))
        } else {
            getVibrantColor(getLightMutedColor(getDominantColor(-1)))
        }.takeIf { it != -1 }?.let { argb -> Color(argb) }
    } ?: backgroundColor
    BoxWithConstraints(modifier) {
        val radialColorBase by animateColorAsState(
            targetValue = color,
            animationSpec = tween(500)
        )
        Box(
            modifier = remember {
                Modifier.fillMaxSize()
            }.runRemember(radialColorBase, compositeBase, constraints) {
                background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            radialColorBase.copy(alpha = 0.55f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.45f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.35f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.2f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.15f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.1f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.05f).compositeOver(compositeBase),
                            radialColorBase.copy(alpha = 0.0f).compositeOver(compositeBase)
                        ),
                        center = Offset(
                            constraints.maxWidth.toFloat() / 2,
                            constraints.maxHeight.toFloat() / 3.5f
                        ),
                        radius = constraints.maxWidth.toFloat() * 0.9f
                    )
                )
            }
        )
    }
}