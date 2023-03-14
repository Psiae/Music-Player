package com.flammky.musicplayer.base.media.playback.real

import android.os.Looper
import com.flammky.musicplayer.base.media.playback.*
import kotlin.time.Duration

class RealPlaybackController(
	private val playbackLooper: Looper,
) : PlaybackController {

	init {
	}












































































































	private var _playing:Boolean = false
	private var _playWhenReady: Boolean = false
	private var _repeatMode: RepeatMode = RepeatMode.OFF
	private var _shuffleMode: ShuffleMode = ShuffleMode.OFF
	private var _queue: OldPlaybackQueue = OldPlaybackQueue.UNSET

	override val looper: Looper
		get() = TODO("Not yet implemented")

	override suspend fun getPlaybackSpeed(): Float {
		TODO("Not yet implemented")
	}

	override suspend fun getDuration(): Duration {
		TODO("Not yet implemented")
	}

	override suspend fun getBufferedPosition(): Duration {
		TODO("Not yet implemented")
	}

	override suspend fun getPosition(): Duration {
		TODO("Not yet implemented")
	}

	override suspend fun getShuffleMode(): ShuffleMode {
		TODO("Not yet implemented")
	}

	override suspend fun getRepeatMode(): RepeatMode {
		TODO("Not yet implemented")
	}

	override suspend fun getQueue(): OldPlaybackQueue {
		TODO("Not yet implemented")
	}

	override suspend fun isPlayWhenReady(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun isPlaying(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun <R> withLooperContext(block: suspend PlaybackController.() -> R): R {
		TODO("Not yet implemented")
	}

	override suspend fun play(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun seekNext(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun seekPrevious(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun seekPreviousMediaItem(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun getPlaybackProperties(): PlaybackProperties {
		TODO("Not yet implemented")
	}

	override suspend fun requestMoveAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun requestSeekIndexAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Boolean {
		TODO("Not yet implemented")
	}

	override fun acquireObserver(owner: Any): PlaybackController.Observer {
		TODO("Not yet implemented")
	}

	override fun releaseObserver(owner: Any) {
		TODO("Not yet implemented")
	}

	override fun hasObserver(owner: Any): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setPlayWhenReady(playWhenReady: Boolean): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setQueue(queue: OldPlaybackQueue): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setRepeatMode(mode: RepeatMode): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun setShuffleMode(mode: ShuffleMode): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun toggleRepeatMode(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun toggleShuffleMode(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun seekPosition(progress: Duration): Boolean {
		TODO("Not yet implemented")
	}


	override suspend fun seekPosition(
		expectId: String,
		expectDuration: Duration,
		percent: Float
	): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun seekIndex(index: Int, startPosition: Duration): Boolean {
		TODO("Not yet implemented")
	}

	override fun inLooper(): Boolean {
		TODO("Not yet implemented")
	}

	private fun checkInLooper() {
		check(Looper.myLooper() == playbackLooper)
	}
}
