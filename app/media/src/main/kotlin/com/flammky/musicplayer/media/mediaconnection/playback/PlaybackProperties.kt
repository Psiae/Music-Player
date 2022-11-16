package com.flammky.musicplayer.media.mediaconnection.playback

data class PlaybackProperties(
	val playWhenReady: Boolean,
	val playing: Boolean,
	val shuffleEnabled: Boolean,
	val hasNextMediaItem: Boolean,
	val hasPreviousMediaItem: Boolean,
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
}
