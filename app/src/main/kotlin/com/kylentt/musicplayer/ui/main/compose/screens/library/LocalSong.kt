package com.kylentt.musicplayer.ui.main.compose.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import com.kylentt.musicplayer.ui.main.compose.screens.library.old.LibraryViewModelOld
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper

data class LocalSongModel(
	val id: String,
	val displayName: String,
	val mediaItem: MediaItem
) {
	private object ART_UNSET

	private val _artState: MutableState<Any?> = mutableStateOf(ART_UNSET)

	val artState: State<Any?> = _artState

	fun updateArt(art: Any?) {

	}
}

@Composable
fun LocalSongRow(viewModel: LibraryViewModelOld) {

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
