package com.flammky.android.medialib.player

import androidx.media3.exoplayer.ExoPlayer
import com.flammky.android.medialib.common.mediaitem.MediaItem
import kotlin.time.Duration

interface LibraryPlayer {
	val playbackInfo: PlaybackInfo
	val timelineInfo: TimelineInfo

	fun play(item: MediaItem)
	fun prepare()
	fun pause()
	fun stop()

	/**
	 * Information about current Playback
	 */
	class PlaybackInfo(

		/**
		 * The Current Buffered Position, wrapped in [kotlin.time.Duration]
		 */
		val bufferedPosition: Duration,

		/**
		 * The Current Position, wrapped in [kotlin.time.Duration]
		 */
		val position: Duration,

		/**
		 * The current MediaItem, null if none is set
		 */
		val mediaItem: MediaItem?,

		/**
		 * Whether the player will start playing when [state] is [PlayerState.READY]
		 */
		val playWhenReady: Boolean,

		/**
		 * Whether the player is playing, i.e whether [position] advances
		 */
		val playing: Boolean,

		/**
		 * The current State of the Player.
		 * @see [LibraryPlayer.PlayerState]
		 */
		val state: PlayerState
	)

	/**
	 * Information about current Timeline
	 */
	class TimelineInfo(
		val windowInfo: WindowInfo
	)

	/**
	 * Information about current Window
	 */
	class WindowInfo(
		val periodInfo: PeriodInfo
	)

	/**
	 * Information about current Period
	 */
	class PeriodInfo(

	)

	fun setMediaItem() {
		(Any() as ExoPlayer).currentPeriodIndex
	}

	/**
	 * The State
	 */
	sealed interface PlayerState {

		/**
		 * Player is IDLE, and needs call to [prepare] in order to start playback
		 */
		object IDLE : PlayerState

		/**
		 * Player is BUFFERING, playback is paused temporarily while it buffers for resources in order
		 * to comply with its Playback configuration requirement.
		 *
		 * @see // TODO
		 */
		object BUFFERING : PlayerState

		/**
		 * Player is READY, playback will be resumed when [PlaybackInfo.playWhenReady] is true
		 */
		object READY : PlayerState

		/**
		 * Player is ENDED, playback has reached ends of possible both timeline and position
		 */
		object ENDED : PlayerState

		companion object {
			@JvmStatic fun PlayerState.isIdle() = this === IDLE
			@JvmStatic fun PlayerState.isBuffering() = this === BUFFERING
			@JvmStatic fun PlayerState.isReady() = this === READY
			@JvmStatic fun PlayerState.isEnded() = this === ENDED

			/**
			 * Whether the Player need to Prepare its resources in order to start playback.
			 *
			 * @see [LibraryPlayer.prepare]
			 */
			@JvmStatic fun PlayerState.requirePrepare() = isIdle()

			/**
			 * Whether the Player need to seek back to its default position in order to start playback
			 */
			@JvmStatic fun PlayerState.requireReposition() = this === ENDED
		}
	}
}
