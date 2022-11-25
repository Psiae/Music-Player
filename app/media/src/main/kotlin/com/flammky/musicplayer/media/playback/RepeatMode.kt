package com.flammky.musicplayer.media.playback

sealed interface RepeatMode {

	object OFF : RepeatMode

	object ONE : RepeatMode

	object ALL : RepeatMode
}
