package com.flammky.musicplayer.media.playback

import android.os.Looper
import kotlin.time.Duration

interface PlaybackController {
	val looper: Looper
	val playing: Boolean
	val playWhenReady: Boolean
	val queue: PlaybackQueue
	val repeatMode: RepeatMode
	val shuffleMode: ShuffleMode
	val progress: Duration
	val bufferedProgress: Duration
	val duration: Duration
	val playbackSpeed: Float

	fun setPlayWhenReady(playWhenReady: Boolean): Boolean
	fun setQueue(queue: PlaybackQueue): Boolean
	fun setRepeatMode(mode: RepeatMode): Boolean
	fun setShuffleMode(mode: ShuffleMode): Boolean
	fun seekProgress(progress: Duration): Boolean
	fun seekIndex(index: Int, startPosition: Duration): Boolean

	/**
	 * Acquire Observer for the specified owner, Get Or Create
	 */
	fun acquireObserver(owner: Any): Observer

	fun releaseObserver(owner: Any)
	fun hasObserver(owner: Any): Boolean

	fun inLooper(): Boolean

	suspend fun <R> withContext(block: suspend PlaybackController.() -> R): R

	/**
	 * Observer interface for this controller, callbacks will be called from the controller Looper
	 */
	interface Observer {
		fun getAndObserveRepeatModeChange(
			onRepeatModeChange: (PlaybackEvent.RepeatModeChange) -> Unit
		): RepeatMode
		fun getAndObserveShuffleModeChange(
			onShuffleModeChange: (PlaybackEvent.ShuffleModeChange) -> Unit
		): ShuffleMode
		fun getAndObserveQueueChange(
			onQueueChange: (PlaybackEvent.QueueChange) -> Unit
		): PlaybackQueue
		fun getAndObserveIsPlayingChange(
			onIsPlayingChange: (PlaybackEvent.IsPlayingChange) -> Unit
		): Boolean
		fun getAndObservePlaybackSpeed(
			onPlaybackSpeedChange: (PlaybackEvent.PlaybackSpeedChange) -> Unit
		): Float
		fun observeDiscontinuity(
			onDiscontinuity: (PlaybackEvent.ProgressDiscontinuity) -> Unit
		)
		fun getAndObserveDurationChange(
			onDurationChange: (Duration) -> Unit
		): Duration

		fun release()
	}
}
