package com.flammky.musicplayer.media.mediaconnection.playback

import kotlinx.collections.immutable.ImmutableList

data class PlaylistInfo(
	val index: Int,
	val playlist: ImmutableList<String>
)
