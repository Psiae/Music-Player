package com.flammky.musicplayer.media.playback

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlaybackQueue(
	val list: ImmutableList<String>,
	val current: Int
) {
	companion object {
		val UNSET = PlaybackQueue(
			list = persistentListOf(),
			current = PlaybackConstants.INDEX_UNSET
		)
	}
}
