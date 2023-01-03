package com.flammky.musicplayer.base.media.mediaconnection.tracks

import com.flammky.android.medialib.common.mediaitem.MediaMetadata

data class TrackMetadata(
	val id: String,
	val artwork: Any?,
	val mediaMetadata: MediaMetadata?
) {
	object NO_ARTWORK
	companion object {
		val UNSET = TrackMetadata(id = "", artwork = null, mediaMetadata = null)
	}
}
