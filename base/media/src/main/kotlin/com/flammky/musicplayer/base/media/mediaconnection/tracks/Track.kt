package com.flammky.musicplayer.base.media.mediaconnection.tracks

class Track(
	val id: String,
	val metadata: TrackMetadata
) {

	companion object {
		val UNSET = Track("", TrackMetadata.UNSET)
	}
}
