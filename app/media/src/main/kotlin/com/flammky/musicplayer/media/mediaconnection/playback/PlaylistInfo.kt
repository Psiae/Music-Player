package com.flammky.musicplayer.media.mediaconnection.playback

import kotlinx.collections.immutable.ImmutableList

data class PlaylistInfo(
	val index: Int,
	val playlist: ImmutableList<String>,
	val changeReason: ChangeReason
) {

	sealed interface ChangeReason {
		object UNKNOWN : ChangeReason
		object MEDIA_TRANSITION : ChangeReason
		object PLAYLIST_CHANGE : ChangeReason
	}
}
