package com.flammky.musicplayer.library.localmedia.ui

import kotlin.time.Duration

data class LocalSongItem(
	val id: String,
	val displayName: String?,
	val artistName: String?,
	val albumName: String?,
	val duration: Duration,
)
