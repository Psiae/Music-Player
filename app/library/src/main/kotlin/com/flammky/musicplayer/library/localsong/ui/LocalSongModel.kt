package com.flammky.musicplayer.library.localsong.ui

import kotlin.time.Duration

sealed class LocalSongModel(
	val id: String,
	val displayName: String?,
	val artistName: String?,
	val albumName: String?,
	val duration: Duration,
)
