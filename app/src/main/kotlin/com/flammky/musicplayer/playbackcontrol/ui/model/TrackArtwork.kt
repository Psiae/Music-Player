package com.flammky.musicplayer.playbackcontrol.ui.model

data class TrackArtwork(
	val id: String,
	val art: Any?
) {
	object NO_ART
	object CORRUPT
	object TOO_BIG

	companion object {
		val UNSET = TrackArtwork("", null)
	}
}
