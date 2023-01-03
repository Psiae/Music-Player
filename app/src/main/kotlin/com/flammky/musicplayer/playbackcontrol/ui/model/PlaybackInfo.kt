package com.flammky.musicplayer.playbackcontrol.ui.model

import com.flammky.musicplayer.core.media.MediaConstants
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

// Think of better name
sealed class PlayPauseCommand(open val enabled: Boolean) {
	data class Play(override val enabled: Boolean) : PlayPauseCommand(enabled)
	data class Pause(override val enabled: Boolean) : PlayPauseCommand(enabled)

	companion object {
		val UNSET = Play(false)
	}
}

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
		val infoChangeReason: ChangeReason,
		val progress: Duration,
		val bufferedProgress: Duration,
		val duration: Duration
	) {
		sealed interface ChangeReason {
			object UNKNOWN : ChangeReason
			object PERIODIC : ChangeReason
			object SEEK_REQUEST : ChangeReason
			object MEDIA_TRANSITION : ChangeReason
			object PROGRESS_DISCONTINUITY : ChangeReason
		}
		companion object {
			val UNSET = Position(
				infoChangeReason = ChangeReason.UNKNOWN,
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
		val canSeekNext: Boolean,
		val canSeekPrevious: Boolean,
		val repeatMode: RepeatMode,
		val playbackState: PlaybackState
	) {
		companion object {
			val UNSET = Properties(
				playWhenReady = false,
				playing = false,
				shuffleEnabled = false,
				canSeekNext = false,
				canSeekPrevious = false,
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

object PlaybackCommands {


}

// Is this unnecessary ?, would like to remove any domain import in UI component except presenter
object PlaybackProperties {

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

	sealed interface ShuffleMode {

		object OFF : ShuffleMode

		object ON : ShuffleMode

		companion object {
			inline val ShuffleMode.on: Boolean
				get() = this == ON
			inline val ShuffleMode.off: Boolean
				get() = this == OFF
		}
	}



	@JvmInline
	value class Progress(val value: kotlin.time.Duration) {

		companion object {
			val UNSET = Progress(PlaybackConstants.POSITION_UNSET)
		}
	}

	@JvmInline
	value class BufferedProgress(val value: kotlin.time.Duration) {

		companion object {
			val UNSET = BufferedProgress(PlaybackConstants.POSITION_UNSET)
		}
	}

	@JvmInline
	value class Duration(val value: kotlin.time.Duration) {

		companion object {
			val UNSET = Duration(PlaybackConstants.DURATION_UNSET)
		}
	}
}
