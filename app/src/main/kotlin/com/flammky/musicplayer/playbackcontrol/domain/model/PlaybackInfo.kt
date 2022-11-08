package com.flammky.musicplayer.playbackcontrol.domain.model

import com.flammky.musicplayer.core.media.MediaConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

data class PlaybackInfo(
	val position: Position,
	val playlist: Playlist,
	val properties: Properties
) {

	data class Playlist(
		val infoChangeReason: PlaylistInfoChangeReason,
		val currentIndex: Int,
		val list: ImmutableList<String>,
	) {
		companion object {
			val UNSET = Playlist(
				infoChangeReason = PlaylistInfoChangeReason.UNKNOWN,
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

	companion object {
		val UNSET = PlaybackInfo(Position.UNSET, Playlist.UNSET, Properties.UNSET)
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

sealed interface PlaylistInfoChangeReason {
	object UNKNOWN : PlaylistInfoChangeReason
	object MEDIA_TRANSITION : PlaylistInfoChangeReason
	object PLAYLIST_CHANGE : PlaylistInfoChangeReason
}
