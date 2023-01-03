package com.flammky.musicplayer.base.media.mediaconnection.playback

data class PlaybackProperties(
	val playWhenReady: Boolean,
	val playing: Boolean,
	val shuffleEnabled: Boolean,
	val canSeekNext: Boolean,
	val canSeekPrevious: Boolean,
	val repeatMode: RepeatMode,
	val playbackState: PlaybackState
) {

	sealed interface RepeatMode {

		/**
		 * No Repeat Mode
		 */
		object OFF : RepeatMode

		/**
		 * Repeat currently playing Media Item
		 */
		object ONE : RepeatMode

		/**
		 * Repeat the Playlist
		 */
		object ALL : RepeatMode
	}

	sealed interface PlaybackState {
		object IDLE : PlaybackState
		object BUFFERING : PlaybackState
		object READY : PlaybackState
		object ENDED : PlaybackState
		object ERROR : PlaybackState
	}

	companion object {
		val UNSET = PlaybackProperties(
			playWhenReady = false,
			playing = false,
			shuffleEnabled = false,
			canSeekNext = false,
			canSeekPrevious = false,
			repeatMode = RepeatMode.OFF,
			playbackState = PlaybackState.IDLE,
		)
	}
}
