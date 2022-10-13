package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

val LocalBottomOffsetVisibility = compositionLocalOf<Dp>{ 0.dp }

@Composable
fun ProvideLocalBottomOffsetVisibility(
	initialValue: Dp = 0.dp,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(LocalBottomOffsetVisibility provides initialValue) {
		content()
	}
}

class VisibilityViewModel() : ViewModel() {
	val bottomVisibilityOffset = mutableStateOf(0.dp)
}


