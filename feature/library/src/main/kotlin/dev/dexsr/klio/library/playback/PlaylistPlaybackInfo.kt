package dev.dexsr.klio.library.playback

import dev.dexsr.klio.base.UNSET

data class PlaylistPlaybackInfo(
	val playing: Boolean,
	val canPause: Boolean,
	val canPlay: Boolean
): UNSET<PlaylistPlaybackInfo> by Companion {

	companion object : UNSET<PlaylistPlaybackInfo> {

		override val UNSET: PlaylistPlaybackInfo = PlaylistPlaybackInfo(
			playing = false,
			canPause = false,
			canPlay = false
		)
	}
}
