package com.flammky.musicplayer.base.media.playback

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlaybackQueue(
	val list: ImmutableList<String>,
	val currentIndex: Int
) {

	init {
		if (list.isEmpty() || currentIndex < 0) {
			require(currentIndex == PlaybackConstants.INDEX_UNSET) {
				"invalid currentIndex=$currentIndex must be " +
					"PlaybackConstants.INDEX_UNSET=${PlaybackConstants.INDEX_UNSET}"
			}
		} else {
			require(currentIndex in list.indices) {
				"currentIndex=$currentIndex must be within the list range=${list.indices}"
			}
		}
	}

	data class Item(
		val id: String,
		val qId: String
	)

	companion object {
		val UNSET = PlaybackQueue(
			list = persistentListOf(),
			currentIndex = PlaybackConstants.INDEX_UNSET
		)
	}
}
