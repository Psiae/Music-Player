package com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import kotlin.math.ln

object ColorHelper {

    fun getStatusBarEnforced() = false

    @Composable
    fun getSurfaceIconTint() = MaterialTheme.colorScheme.onSurface

    @Composable
    fun getStatusBarColor() = getTonedSurface().copy(alpha = 0.5f)

    @Composable
    fun getNavBarColor() = getTonedSurface()

    @Composable
    fun getBottomNavigatorColor() = Color.Transparent

    @Composable
    fun getDNTextColor() = if (isSystemInDarkTheme()) ColorDefaults.lightText else ColorDefaults.darkText

    @Composable
    fun getDNBackground() = if (isSystemInDarkTheme()) ColorDefaults.darkThemeBackground else ColorDefaults.lightThemeBackground

    @Composable
    fun getTonedSurface(el: Int = 2) = run {
        val alpha = ((4.5f * ln((el).dp.value + 1)) + 2f) / 100f
        val surface = MaterialTheme.colorScheme.surface
        val primary = MaterialTheme.colorScheme.primary
        primary.copy(alpha = alpha).compositeOver(surface)
    }
}