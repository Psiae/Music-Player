package com.flammky.android.medialib.player

import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.async.AsyncPlayer
import kotlin.time.Duration

/**
 * root Interface for a Player.
 *
 * @see [AsyncPlayer]
 * @see [ThreadLockedPlayer]
 */

interface Player {

	// Playback Properties
	// should we wrap it in a class or create separate interface instead or keep it as is ?

	/**
	 * The current buffered position.
	 *
	 * i.e. the maximum forward seek position that will immediately play without additional buffering.
	 *
	 * wrapped in [kotlin.time.Duration].
	 *
	 * [Contract.DURATION_UNSET] or effectively negative if UNSET
	 */
	val bufferedPosition: Duration

	/**
	 * The current retained buffered position.
	 *
	 * i.e. the minimum backward seek position without buffering
	 *
	 * wrapped in [kotlin.time.Duration].
	 *
	 * [Contract.DURATION_UNSET] or effectively negative if UNSET
	 */
	val retainedBufferedPosition: Duration

	/**
	 * The current playback position,
	 *
	 * wrapped in [kotlin.time.Duration]
	 *
	 * [Contract.DURATION_UNSET] or effectively negative if UNSET
	 */
	val position: Duration

	/**
	 * The current duration of current MediaItem
	 *
	 * [Contract.DURATION_UNSET] or effectively negative if UNSET
	 * [Contract.DURATION_INDEFINITE] if unknown
	 */
	val duration: Duration

	/**
	 * Whether Player will immediately start playback when its ready
	 */
	var playWhenReady: Boolean

	/**
	 * Whether the player is playing, i.e whether [position] advances
	 */
	val isPlaying: Boolean

	/**
	 * The current MediaItem
	 *
	 * null if UNSET
	 */
	val mediaItem: MediaItem?

	/**
	 * The current MediaItem index
	 *
	 * [Contract.INDEX_UNSET] if UNSET
	 */

	val currentMediaItemIndex: Int

	/**
	 * The current State of the Player.
	 * @see [Player.State]
	 */
	val state: State

	/**
	 *
	 */
	fun play()
	fun prepare()
	fun pause()
	fun stop()

	/**
	 * The State, this interface is sealed with open child for convenience
	 */
	sealed interface State {

		/**
		 * Player is IDLE, and needs call to [prepare] in order to start playback
		 */
		open class IDLE : State

		/**
		 * Player is BUFFERING, playback is paused temporarily while it buffers for resources in order
		 * to comply with its Playback configuration requirement.
		 *
		 * @see // TODO
		 */
		open class BUFFERING : State

		/**
		 * Player is READY, playback will be resumed when [playWhenReady] is true
		 */
		open class READY : State

		/**
		 * Player is ENDED, playback has reached ends of possible both timeline and position
		 */
		open class ENDED : State

		/**
		 * Player is on ERROR state, for convenience
		 */
		open class ERROR : State

		companion object {
			@JvmStatic fun State.isIdle() = this is IDLE
			@JvmStatic fun State.isBuffering() = this is BUFFERING
			@JvmStatic fun State.isReady() = this is READY
			@JvmStatic fun State.isEnded() = this is ENDED
		}
	}
}
