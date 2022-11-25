package com.flammky.musicplayer.media.playback

sealed interface ShuffleMode {

	object ON : ShuffleMode

	object OFF : ShuffleMode
}
