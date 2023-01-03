package com.flammky.musicplayer.base.theme.compose

import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun NoRippleColor(content: @Composable () -> Unit) {
	CompositionLocalProvider(
		LocalRippleTheme provides NoRippleTheme,
		content = content
	)
}


private object NoRippleTheme : RippleTheme {

	@Composable
	override fun defaultColor(): Color = Color.Transparent

	@Composable
	override fun rippleAlpha(): RippleAlpha = RippleAlpha(0f, 0f, 0f, 0f)
}
