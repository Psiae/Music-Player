package com.flammky.musicplayer.media.playback.real

import android.os.Looper
import com.flammky.musicplayer.media.playback.*
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
	private var _queue: PlaybackQueue = PlaybackQueue.UNSET


	override val playing: Boolean
		get() {
			checkInLooper()
			return _playing
		}
	override val playWhenReady: Boolean
		get() {
			checkInLooper()
			return _playWhenReady
		}
	override val repeatMode: RepeatMode
		get() {
			checkInLooper()
			return _repeatMode
		}
	override val shuffleMode: ShuffleMode
		get() {
			checkInLooper()
			return _shuffleMode
		}
	override val queue: PlaybackQueue
		get() {
			checkInLooper()
			return _queue
		}

	override val progress: Duration
		get() = TODO("Not yet implemented")
	override val bufferedProgress: Duration
		get() = TODO("Not yet implemented")
	override val duration: Duration
		get() = TODO("Not yet implemented")
	override val playbackSpeed: Float
		get() = TODO("Not yet implemented")

	override val looper: Looper
		get() = TODO("Not yet implemented")

	override fun play(): Boolean {
		TODO("Not yet implemented")
	}

	override fun seekNext(): Boolean {
		TODO("Not yet implemented")
	}

	override fun seekPrevious(): Boolean {
		TODO("Not yet implemented")
	}

	override fun getPlaybackProperties(): PlaybackProperties {
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

	override fun setPlayWhenReady(playWhenReady: Boolean): Boolean {
		TODO("Not yet implemented")
	}

	override fun setQueue(queue: PlaybackQueue): Boolean {
		TODO("Not yet implemented")
	}

	override fun setRepeatMode(mode: RepeatMode): Boolean {
		TODO("Not yet implemented")
	}

	override fun setShuffleMode(mode: ShuffleMode): Boolean {
		TODO("Not yet implemented")
	}

	override fun seekProgress(progress: Duration): Boolean {
		TODO("Not yet implemented")
	}

	override fun seekIndex(index: Int, startPosition: Duration): Boolean {
		TODO("Not yet implemented")
	}

	override fun inLooper(): Boolean {
		TODO("Not yet implemented")
	}

	override suspend fun <R> withContext(block: suspend PlaybackController.() -> R): R {
		TODO("Not yet implemented")
	}

	private fun checkInLooper() {
		check(Looper.myLooper() == playbackLooper)
	}
}
