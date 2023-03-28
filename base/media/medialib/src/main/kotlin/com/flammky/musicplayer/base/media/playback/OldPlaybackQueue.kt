package com.flammky.musicplayer.base.media.playback

import android.net.Uri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Deprecated("Slowly integrate snapshot_id and queue-based identifier")
class OldPlaybackQueue(
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

	fun copy(
		list: ImmutableList<String> = this.list,
		currentIndex: Int = this.currentIndex
	): OldPlaybackQueue {
		return OldPlaybackQueue(list, currentIndex)
	}

	data class Item(
		val id: String,
		val qId: String,
		val uri: Uri
	)

	data class List(
		val snapshotId: String,
		val items: ImmutableList<String>
	)

	companion object {
		val UNSET = OldPlaybackQueue(
			list = persistentListOf(),
			currentIndex = PlaybackConstants.INDEX_UNSET
		)
	}
}
