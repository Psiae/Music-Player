package com.flammky.musicplayer.base.media.playback

import android.os.Looper
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface PlaybackController {
	val looper: Looper
	suspend fun getPlaybackSpeed(): Float
	suspend fun getDuration(): Duration
	suspend fun getBufferedPosition(): Duration
	suspend fun getPosition(): Duration
	suspend fun getShuffleMode(): ShuffleMode
	suspend fun getRepeatMode(): RepeatMode
	suspend fun getQueue(): OldPlaybackQueue
	suspend fun isPlayWhenReady(): Boolean
	suspend fun isPlaying(): Boolean
	suspend fun setPlayWhenReady(playWhenReady: Boolean): Boolean
	suspend fun setQueue(queue: OldPlaybackQueue): Boolean
	suspend fun setRepeatMode(mode: RepeatMode): Boolean
	suspend fun setShuffleMode(mode: ShuffleMode): Boolean
	suspend fun toggleRepeatMode(): Boolean
	suspend fun toggleShuffleMode(): Boolean
	suspend fun seekPosition(progress: Duration): Boolean
	suspend fun seekPosition(expectId: String, expectDuration: Duration, percent: Float): Boolean
	suspend fun seekIndex(index: Int, startPosition: Duration): Boolean
	suspend fun play(): Boolean
	suspend fun seekNext(): Boolean
	suspend fun seekPrevious(): Boolean
	suspend fun seekPreviousMediaItem(): Boolean

	suspend fun getPlaybackProperties(): PlaybackProperties

	suspend fun requestMoveAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Boolean

	suspend fun requestSeekIndexAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Boolean

	/**
	 * Acquire Observer for the specified owner, Get Or Create
	 */
	fun acquireObserver(owner: Any): Observer

	fun releaseObserver(owner: Any)
	fun hasObserver(owner: Any): Boolean

	fun inLooper(): Boolean

	suspend fun <R> withLooperContext(block: suspend PlaybackController.() -> R): R

	/**
	 * Observer interface for this controller, callbacks will be called from the controller Looper
	 */
	interface Observer {
		fun observeRepeatMode(): Flow<RepeatMode>
		fun observeShuffleMode(): Flow<ShuffleMode>
		fun observeQueue(): Flow<OldPlaybackQueue>
		fun observeIsPlaying(): Flow<Boolean>
		fun observePlaybackSpeed(): Flow<Float>
		fun observePositionDiscontinuityEvent(): Flow<PlaybackEvent.PositionDiscontinuity>
		fun observeDuration(): Flow<Duration>
		fun observePlaybackProperties(): Flow<PlaybackProperties>
	}
}
