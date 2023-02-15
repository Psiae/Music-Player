package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO: Define these clearly, whether system insets should be part of it
object LocalLayoutVisibility {
	val LocalBottomBar = compositionLocalOf<Dp> { 0.dp }
	val LocalTopBar =  compositionLocalOf<Dp> { 0.dp }
}
