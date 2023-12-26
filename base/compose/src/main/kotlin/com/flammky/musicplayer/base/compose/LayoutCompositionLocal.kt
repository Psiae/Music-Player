package com.flammky.musicplayer.base.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// TODO: Define these clearly, whether system insets should be part of it
object LocalLayoutVisibility {
	@SuppressLint("CompositionLocalNaming")
	val Bottom = compositionLocalOf<Dp> { 0.dp }
	@SuppressLint("CompositionLocalNaming")
	val Top =  compositionLocalOf<Dp> { 0.dp }
}
