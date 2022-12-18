package com.flammky.musicplayer.media.playback

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlaybackQueue(
	val list: ImmutableList<String>,
	val currentIndex: Int
) {
	companion object {
		val UNSET = PlaybackQueue(
			list = persistentListOf(),
			currentIndex = PlaybackConstants.INDEX_UNSET
		)
	}
}
