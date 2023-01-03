package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object LocalLayoutVisibility {
	val LocalBottomBar = compositionLocalOf<Dp> { 0.dp }
	val LocalTopBar =  compositionLocalOf<Dp> { 0.dp }
}
