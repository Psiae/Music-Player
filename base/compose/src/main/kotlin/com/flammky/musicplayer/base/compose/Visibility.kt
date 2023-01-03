package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

class VisibilityViewModel() : ViewModel() {
	val bottomVisibilityOffset = mutableStateOf(0.dp)

	init {
	}
}
