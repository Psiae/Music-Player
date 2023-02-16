package com.flammky.musicplayer.main.ui.compose

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.main.MainActivity
import com.flammky.musicplayer.main.ui.compose.entry.EntryGuard

fun MainActivity.setContent() = setContent {
	ThemedContent()
}

@Composable
private fun MainActivity.ThemedContent() = MaterialDesign3Theme {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Theme.backgroundColorAsState().value),
	) {
		EntryGuard {
		}
	}
}
