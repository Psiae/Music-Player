package com.flammky.musicplayer.library.localsong.data

import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.mediaitem.MediaItem

@Immutable
data class LocalSongModel(
	val id: String,
	val displayName: String?,
	val fileInfo: FileInfo,
	val mediaItem: MediaItem
) {

	@Immutable
	data class FileInfo(
		val fileName: String?,
		// More
	)
}
