package com.flammky.musicplayer.playbackcontrol.ui.model

import com.flammky.musicplayer.core.media.MediaConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

object PlaybackInfo {


	// should we have Playlist and Position Together ?
	data class Playlist(
		val infoChangeReason: ChangeReason,
		val currentIndex: Int,
		val list: ImmutableList<String>,
	) {
		sealed interface ChangeReason {
			object UNKNOWN : ChangeReason
			object MEDIA_TRANSITION : ChangeReason
			object PLAYLIST_CHANGE : ChangeReason
		}
		companion object {
			val UNSET = Playlist(
				infoChangeReason = ChangeReason.UNKNOWN,
				currentIndex = MediaConstants.INDEX_UNSET,
				list = persistentListOf()
			)
		}
	}

	data class Position(
		val infoChangeReason: PositionInfoChangeReason,
		val progress: Duration,
		val bufferedProgress: Duration,
		val duration: Duration
	) {
		companion object {
			val UNSET = Position(
				infoChangeReason = PositionInfoChangeReason.UNKNOWN,
				progress = MediaConstants.POSITION_UNSET,
				bufferedProgress = MediaConstants.POSITION_UNSET,
				duration = MediaConstants.DURATION_UNSET
			)
		}
	}

	data class Properties(
		val playWhenReady: Boolean,
		val playing: Boolean,
		val shuffleEnabled: Boolean,
		val hasNextMediaItem: Boolean,
		val hasPreviousMediaItem: Boolean,
		val repeatMode: RepeatMode,
		val playbackState: PlaybackState
	) {
		companion object {
			val UNSET = Properties(
				playWhenReady = false,
				playing = false,
				shuffleEnabled = false,
				hasNextMediaItem = false,
				hasPreviousMediaItem = false,
				repeatMode = RepeatMode.OFF,
				playbackState = PlaybackState.IDLE
			)
		}
	}

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


sealed interface PositionInfoChangeReason {
	object UNKNOWN : PositionInfoChangeReason
	object PERIODIC : PositionInfoChangeReason
	object SEEK_REQUEST : PositionInfoChangeReason
	object MEDIA_TRANSITION : PositionInfoChangeReason
	object PROGRESS_DISCONTINUITY : PositionInfoChangeReason
}


