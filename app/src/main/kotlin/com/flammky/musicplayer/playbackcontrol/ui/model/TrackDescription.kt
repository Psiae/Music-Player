package com.flammky.musicplayer.playbackcontrol.ui.model

data class TrackDescription(
	val id: String,
	val title: String,
	val subtitle: String
) {

	companion object {
		val UNSET = TrackDescription("", "", "")
	}
}
