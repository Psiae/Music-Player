package com.kylentt.mediaplayer.core.media3.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.media3.MediaItemFactory

data class PlaybackState(
	val mediaItem: MediaItem,
	val playWhenReady: Boolean,
	val playing: Boolean,
	@Player.RepeatMode val playerRepeatMode: Int,
	@Player.State val playerState: Int
) {

	companion object {
		val EMPTY = PlaybackState(MediaItemFactory.EMPTY, playWhenReady = false, playing = false,
			playerRepeatMode = Player.REPEAT_MODE_OFF, playerState = Player.STATE_IDLE
		)
	}
}
