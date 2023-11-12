package com.flammky.musicplayer.base.media.playback

sealed interface RepeatMode {

	object OFF : RepeatMode

	object ONE : RepeatMode

	object ALL : RepeatMode
}

fun RepeatMode.toIntConstants(): Int = when(this) {
	is RepeatMode.OFF -> 0
	is RepeatMode.ONE -> 1
	is RepeatMode.ALL -> 2
}
