package com.flammky.musicplayer.root

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.main.MainActivity

internal fun MainActivity.setRootContent() = setContent {
	MaterialDesign3Theme {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Theme.backgroundColorAsState().value),
		) {
			RootEntry(viewModel())
		}
	}
}
