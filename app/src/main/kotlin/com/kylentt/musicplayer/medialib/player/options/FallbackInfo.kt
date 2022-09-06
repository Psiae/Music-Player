package com.kylentt.musicplayer.medialib.player.options

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player.Commands
import androidx.media3.common.Timeline
import com.kylentt.musicplayer.medialib.player.LibraryPlayer
import com.kylentt.musicplayer.medialib.player.contract.UNSET
import com.kylentt.musicplayer.medialib.player.playback.RepeatMode

/**
 * Preferences on fallback values.
 *
 * &nbsp;
 *
 * Internally our player still use pre-defined constants that is publicly available,
 * this only have effect on information(s) that is sent publicly.
 *
 * ```
 * e.g: when there's no MediaItem playing, calling LibraryPlayer.duration on LibraryPlayer
 * that has FallbackInfo.noDurationMs set to -1, instead of our internal Long.MIN_VALUE + 1, -1 will be given.
 * ```
 *
 * This class also has effect on publicly added listeners.
 *
 * &nbsp;
 *
 * Default to value used internally
 * @see [UNSET]
 *
 */
// TODO
class FallbackInfo private constructor(
	val noAvailableCommands: Commands,
	val noMediaItem: MediaItem,
	val noPlaybackParameters: PlaybackParameters,
	val noPlaybackState: LibraryPlayer.PlaybackState,
	val noRepeatMode: RepeatMode,
	val noTimeline: Timeline,
	val noDurationMs: Long,
	val noPositionMs: Long,
	val noIndex: Int,
) {

	class Builder() {
		private var mNoAvailableCommands: Commands = Commands.EMPTY
		private var mNoMediaItem: MediaItem = MediaItem.EMPTY
		private var mNoPlaybackParameters: PlaybackParameters = PlaybackParameters.DEFAULT
		private var mNoPlaybackState: LibraryPlayer.PlaybackState = LibraryPlayer.PlaybackState.IDLE
		private var mNoRepeatMode: RepeatMode = RepeatMode.NONE
		private var mNoTimeline: Timeline = Timeline.EMPTY
		private var mNoDurationMs: Long = UNSET.Time
		private var mNoPositionMs: Long = UNSET.Position
		private var mNoIndex: Int = UNSET.Index

		fun setNoAvailableCommands(noAvailableCommands: Commands): Builder {
			mNoAvailableCommands = noAvailableCommands
			return this
		}

		fun setNoPlaybackParameters(noPlaybackParameters: PlaybackParameters): Builder {
			mNoPlaybackParameters = noPlaybackParameters
			return this
		}

		fun setNoPlaybackState(noPlaybackState: LibraryPlayer.PlaybackState): Builder {
			mNoPlaybackState = noPlaybackState
			return this
		}

		fun setNoRepeatMode(noRepeatMode: RepeatMode): Builder {
			mNoRepeatMode = noRepeatMode
			return this
		}

		fun setNoTimeLine(noTimeLine: Timeline): Builder {
			mNoTimeline = noTimeLine
			return this
		}

		fun setNoDurationMs(noDurationMs: Long): Builder {
			mNoDurationMs = noDurationMs
			return this
		}

		fun setNoPositionMs(noPositionMs: Long): Builder {
			mNoPositionMs = noPositionMs
			return this
		}

		fun setNoIndex(noIndex: Int): Builder {
			mNoIndex = noIndex
			return this
		}

		fun build(): FallbackInfo {
			return FallbackInfo(
				noAvailableCommands = mNoAvailableCommands,
				noMediaItem = mNoMediaItem,
				noPlaybackParameters = mNoPlaybackParameters,
				noPlaybackState = mNoPlaybackState,
				noRepeatMode = mNoRepeatMode,
				noTimeline = mNoTimeline,
				noDurationMs = mNoDurationMs,
				noPositionMs = mNoPositionMs,
				noIndex = mNoIndex
			)
		}
	}

	companion object {
		val DEFAULT = Builder().build()
	}
}
