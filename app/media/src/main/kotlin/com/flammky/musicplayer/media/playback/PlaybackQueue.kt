package com.flammky.musicplayer.media.playback

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
					"PlaybackConstants.INDEX_UNSET=${PlaybackConstants.DURATION_UNSET}"
			}
		} else {
			requireNotNull(list.getOrNull(currentIndex)) {
				"currentIndex=$currentIndex must be within the list range=${list.indices}"
			}
		}
	}

	companion object {
		val UNSET = PlaybackQueue(
			list = persistentListOf(),
			currentIndex = PlaybackConstants.INDEX_UNSET
		)
	}
}
