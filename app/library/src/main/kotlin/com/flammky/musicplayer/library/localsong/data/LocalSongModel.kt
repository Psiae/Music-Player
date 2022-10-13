package com.flammky.musicplayer.library.localsong.data

import com.flammky.android.medialib.common.mediaitem.MediaItem

sealed class LocalSongModel(
	val id: String,
	val displayName: String?,
	val mediaItem: MediaItem
)
