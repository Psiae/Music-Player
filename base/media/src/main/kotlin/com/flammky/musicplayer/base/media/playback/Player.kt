package com.flammky.musicplayer.base.media.playback

interface Player {
	var playWhenReady: Boolean
	val playing: Boolean
	var shuffleMode: ShuffleMode
	var repeatMode: RepeatMode
}
