package com.flammky.musicplayer.main.presentation.root

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.android.compose.LocalIntentManager
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import dev.dexsr.klio.android.main.MainActivity

internal fun MainActivity.setRootContent() = setContent {
	MaterialDesign3Theme {
		CompositionLocalProvider(
			LocalIntentManager provides intentManager
		) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Theme.backgroundColorAsState().value),
			) {
				RootEntry(viewModel())
			}
		}
	}
}
