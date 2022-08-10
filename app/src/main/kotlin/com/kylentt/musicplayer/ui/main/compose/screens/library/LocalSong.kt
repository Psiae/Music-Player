package com.kylentt.musicplayer.ui.main.compose.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper

@Composable
fun LocalSongRow(viewModel: LibraryViewModel) {

}

@Composable
private fun LocalSongHeader() {
	Box(modifier = Modifier.fillMaxWidth()) {
		val type = MaterialTheme.typography.titleMedium
		Text(
			text = "Your Local Song",
			color = ColorHelper.textColor(),
			style = type
		)
	}
}
