package com.flammky.musicplayer.media.mediaconnection.playback

import com.flammky.musicplayer.media.playback.PlaybackEvent
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.media.playback.RepeatMode
import com.flammky.musicplayer.media.playback.ShuffleMode
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration


/**
 * Playback interface to be used outside of domain
 */
interface PlaybackConnection {

	// Session Aware ?

	/**
	 * Join the Controller dispatcher queue
	 */
	suspend fun <R> withControllerContext(
		block: suspend Controller.() -> R
	): R

	/**
	 * Join the Controller dispatcher immediate queue (immediate dispatching)
	 */
	suspend fun <R> withControllerImmediateContext(
		block: suspend Controller.() -> R
	): R

	/**
	 * Post suspending runnable to the Controller
	 */
	fun postController(block: suspend Controller.() -> Unit)

	/**
	 * Post suspending runnable to the Controller immediately
	 */
	fun immediatePostController(block: suspend Controller.() -> Unit)

	/**
	 * Post suspending runnable to the Controller with Future result
	 */
	fun <R> postControllerCallback(block: suspend Controller.() -> R): ListenableFuture<R>

	/**
	 * Post suspending runnable to the Controller immediately with Future result
	 */
	fun <R> immediatePostControllerCallback(block: suspend Controller.() -> R): ListenableFuture<R>

	interface Context {
	}

	interface Controller {

		//
		// keep these simple for now
		//

		suspend fun getRepeatMode(): RepeatMode
		suspend fun setRepeatMode(mode: RepeatMode): Boolean
		suspend fun observeRepeatMode(): Flow<RepeatMode>

		suspend fun getShuffleMode(): ShuffleMode
		suspend fun setShuffleMode(mode: ShuffleMode): Boolean
		suspend fun observeShuffleMode(): Flow<ShuffleMode>

		suspend fun getProgress(): Duration
		suspend fun seekProgress(progress: Duration): Boolean
		suspend fun seekIndex(index: Int, startProgress: Duration): Boolean
		suspend fun observeProgressDiscontinuity(): Flow<PlaybackEvent.ProgressDiscontinuity>

		suspend fun getBufferedProgress(): Duration

		suspend fun getIsPlaying(): Boolean
		suspend fun observeIsPlaying(): Flow<Boolean>

		suspend fun getDuration(): Duration
		suspend fun observeDuration(): Flow<Duration>

		suspend fun getPlaybackSpeed(): Float
		suspend fun observePlaybackSpeed(): Flow<Float>

		suspend fun getQueue(): PlaybackQueue
		suspend fun setQueue(queue: PlaybackQueue): Boolean
		suspend fun observeQueue(): Flow<PlaybackQueue>
		suspend fun observeQueueChange(): Flow<PlaybackEvent.QueueChange>
	}
}
