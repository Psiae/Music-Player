package com.flammky.musicplayer.ui.util.compose

import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Composable
fun NoRipple(content: @Composable () -> Unit) {
	CompositionLocalProvider(
		LocalRippleTheme provides NoRippleTheme,
		content = content
	)
}

@Composable
fun <T> T.NoRipple(content: @Composable T.() -> Unit) {
	CompositionLocalProvider(
		LocalRippleTheme provides NoRippleTheme,
	) {
		content()
	}
}

@Immutable
private object NoRippleTheme : RippleTheme {
	@Composable override fun defaultColor(): Color = Color.Transparent
	@Composable override fun rippleAlpha(): RippleAlpha {
		return RippleAlpha(0F,0F,0F,0F)
	}
}
