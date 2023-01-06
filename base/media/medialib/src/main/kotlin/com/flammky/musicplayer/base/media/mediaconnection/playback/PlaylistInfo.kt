package com.flammky.musicplayer.base.media.mediaconnection.playback

import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlaylistInfo(
	val index: Int,
	val playlist: ImmutableList<String>,
	val changeReason: ChangeReason
) {

	companion object {
		val UNSET = PlaylistInfo(
			index = PlaybackConstants.INDEX_UNSET,
			playlist = persistentListOf(),
			changeReason = ChangeReason.UNKNOWN
		)
	}

	sealed interface ChangeReason {
		object UNKNOWN : ChangeReason
		object MEDIA_TRANSITION : ChangeReason
		object PLAYLIST_CHANGE : ChangeReason
	}
}
