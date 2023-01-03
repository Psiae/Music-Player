package com.flammky.musicplayer.base.media.playback

sealed interface ShuffleMode {

	object ON : ShuffleMode

	object OFF : ShuffleMode
}
