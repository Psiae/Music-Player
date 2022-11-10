package com.flammky.musicplayer.playbackcontrol.domain.model

import com.flammky.android.medialib.common.mediaitem.MediaMetadata

data class TrackInfo(
	val id: String,
	val artwork: Any?,
	val mediaMetadata: MediaMetadata?
) {
	object NO_ARTWORK
	companion object {
		val UNSET = TrackInfo("", null, MediaMetadata.UNSET)
		val UNKNOWN = UNSET.copy()
	}
}
