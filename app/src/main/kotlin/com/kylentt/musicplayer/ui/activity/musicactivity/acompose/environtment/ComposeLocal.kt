package com.kylentt.musicplayer.ui.activity.musicactivity.acompose.environtment

import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun NoRipple(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalRippleTheme provides object : RippleTheme {
            @Composable
            override fun defaultColor(): Color = Color.Transparent

            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleAlpha(
                0f, 0f, 0f, 0f
            )
        }
    ) {
        content()
    }
}