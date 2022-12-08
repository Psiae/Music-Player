package com.flammky.musicplayer.media.playback

interface Player {
	var playWhenReady: Boolean
	val playing: Boolean
	var shuffleMode: ShuffleMode
	var repeatMode: RepeatMode
}
