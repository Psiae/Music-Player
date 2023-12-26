package dev.dexsr.klio.library.spotify.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.backgroundColorAsState
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState

@Composable
internal fun SpotifyUiRoot() {
	Box(modifier = Modifier.fillMaxSize()) {
		Text(
			modifier = Modifier.align(Alignment.Center),
			text = "Work In Progress",
			style = MaterialTheme.typography.headlineMedium,
			color = MD3Theme.backgroundContentColorAsState().value
		)
	}
}
