package com.flammky.musicplayer.ui.library.local

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem

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
