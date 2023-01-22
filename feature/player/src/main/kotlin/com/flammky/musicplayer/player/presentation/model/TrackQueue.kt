package com.flammky.musicplayer.player.presentation.model

import androidx.compose.runtime.Immutable
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TrackQueue(
	val currentIndex: Int,
	val list: ImmutableList<String>,
) {
	init {
		// `currentIndex` should be `PlaybackConstants.INDEX_UNSET` when `list` is `empty`
		require(list.isNotEmpty() || currentIndex == PlaybackConstants.INDEX_UNSET) {
			"Invalid Index on empty queue: $this"
		}
		// `currentIndex` should be `in list.indices` when `list` is `not empty`
		require(currentIndex in list.indices) {
			"Invalid Index non-empty queue: $this"
		}
	}

	override fun toString(): String {
		return """
			TrackQueue: ($currentIndex) (${list.size})
									${list.joinToString()}
		"""
	}

	companion object {
		val UNSET = TrackQueue(PlaybackConstants.INDEX_UNSET, persistentListOf())
	}
}
