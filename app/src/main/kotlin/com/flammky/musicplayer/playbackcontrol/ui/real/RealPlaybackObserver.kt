package com.flammky.musicplayer.playbackcontrol.ui.real

import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RealPlaybackObserver(
	private val scope: CoroutineScope,
	private val dispatcher: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver {
	private val progressLock = Any()

	private var preferredProgressCheckInterval: Duration? = null
	private var progressCollector: Job? = null
	private var durationCollector: Job? = null
	private var discontinuityCollector: Job? = null
	private var isPlayingCollector: Job? = null

	private val _progressMSF = MutableStateFlow(PlaybackConstants.PROGRESS_UNSET)
	private val _bufferedProgressMSF = MutableStateFlow(PlaybackConstants.PROGRESS_UNSET)
	private val _durationMSF = MutableStateFlow(PlaybackConstants.DURATION_UNSET)

	override val progressStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _progressMSF.asStateFlow()
		}
	override val bufferedProgressStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _bufferedProgressMSF.asStateFlow()
		}
	override val durationStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _durationMSF.asStateFlow()
		}

	init {
		scope.launch(dispatcher.mainImmediate) {
			combine(
				_progressMSF.subscriptionCount,
				_bufferedProgressMSF.subscriptionCount,
				_durationMSF.subscriptionCount
			) { a, b, c ->
				maxOf(a, b, c)
			}.first {
				it > 0
			}
			dispatchProgressCollector()
			dispatchDurationCollector()
			dispatchDiscontinuityCollector()
			dispatchIsPlayingCollector()
		}
	}

	@MainThread
	override fun setPreferredProgressCollectionDelay(interval: Duration?) {
		checkInMainThread {
			"setPreferredProgressCheckInterval is confined to `Thread-Main`"
		}
		if (preferredProgressCheckInterval == interval) {
			return
		}

		preferredProgressCheckInterval = interval
		if (progressCollector?.isActive == true) {
			dispatchProgressCollector(preferredProgressCheckInterval)
		}
	}

	override suspend fun updatePosition() {
		updateProgress()
	}

	@MainThread
	private fun dispatchProgressCollector(
		startIn: Duration? = null,
	) {
		progressCollector?.cancel()
		progressCollector = scope.launch {
			if (startIn != null) delay(progressCollectionDelay(startIn))
			collectPlaybackProgress()
		}
	}

	@MainThread
	private fun resetProgressCollector() {
		progressCollector?.cancel()
		_progressMSF.value = PlaybackConstants.PROGRESS_UNSET
		_bufferedProgressMSF.value = PlaybackConstants.PROGRESS_UNSET
	}

	@MainThread
	private fun dispatchDurationCollector() {
		durationCollector?.cancel()
		durationCollector = scope.launch {
			collectPlaybackDuration()
		}
	}

	@MainThread
	private fun resetDurationCollector() {
		durationCollector?.cancel()
		_durationMSF.value = PlaybackConstants.DURATION_UNSET
	}

	@MainThread
	private fun dispatchDiscontinuityCollector() {
		discontinuityCollector?.cancel()
		discontinuityCollector = scope.launch {
			collectPlaybackDiscontinuity()
		}
	}

	@MainThread
	private fun dispatchIsPlayingCollector() {
		isPlayingCollector?.cancel()
		isPlayingCollector = scope.launch {
			collectIsPlaying()
		}
	}

	private suspend fun collectPlaybackProgress() {
		do {
			updateProgress()
			delay(progressCollectionDelay(preferredProgressCheckInterval))
		} while (coroutineContext.job.isActive)
	}

	private suspend fun collectPlaybackDuration() {
		playbackConnection.observeDuration().distinctUntilChanged().collect {
			updateDuration()
			updateProgress()
		}
	}

	private suspend fun collectPlaybackDiscontinuity() {
		playbackConnection.observeProgressDiscontinuity().collect {
			updateDuration()
			updateProgress()
		}
	}

	private suspend fun collectIsPlaying() {
		playbackConnection.observeIsPlaying().collect {
			if (!it) {
				progressCollector?.cancel()
			} else if (progressCollector?.isActive != true) {
				dispatchProgressCollector()
			}
		}
	}

	private suspend fun updateDuration() {
		playbackConnection.joinContext {
			_durationMSF.update {
				getDuration().also { Timber.d("RealPlaybackObserver updateDuration $it") }
			}
		}
	}

	private suspend fun updateProgress() {
		playbackConnection.joinContext {
			_progressMSF.update {
				getProgress().also { Timber.d("RealPlaybackObserver updateProgress $it") }
			}
			_bufferedProgressMSF.update {
				getBufferedProgress()
			}
		}
	}

	/**
	 * Calculate the delay
	 *
	 * @param preferred the preferred delay, useful for display related purposes
	 */
	@MainThread
	private suspend fun progressCollectionDelay(
		preferred: Duration?
	): Duration {
		return playbackConnection.joinContext {
			val speed = getPlaybackSpeed()

			require(speed > 0f) {
				"Playback Speed ($speed) should be > 0f"
			}

			val duration = getDuration()
				.takeIf { it != PlaybackConstants.DURATION_UNSET }
				?: Duration.ZERO

			require(duration >= Duration.ZERO) {
				"Playback Duration should be constrained >= ${Duration.ZERO}"
			}

			val progress = getProgress()
				.takeIf { it != PlaybackConstants.PROGRESS_UNSET }
				?: Duration.ZERO

			require(progress >= Duration.ZERO) {
				"Playback Progress should be constrained > ${Duration.ZERO}"
			}

			val left = duration - progress
			val nextSecond = (1000 - progress.inWholeMilliseconds % 1000).milliseconds
			val speedConstrained = minOf(
				preferred ?: MAX_PROGRESS_COLLECTION_INTERVAL,
				left,
				nextSecond
			).inWholeMilliseconds / speed
			speedConstrained.toLong().milliseconds.coerceIn(
				// Should be handled by other callback
				MIN_PROGRESS_COLLECTION_INTERVAL,
				MAX_PROGRESS_COLLECTION_INTERVAL
			)
		}.also {
			Timber.d("RealPlaybackObserver progressCollectionInterval: $it")
			// Safeguard
			check(it in MIN_PROGRESS_COLLECTION_INTERVAL .. MAX_PROGRESS_COLLECTION_INTERVAL) {
				"Delay($it) was !in $MIN_PROGRESS_COLLECTION_INTERVAL .. " +
					"$MAX_PROGRESS_COLLECTION_INTERVAL inclusive"
			}
		}
	}


	override fun release() {

	}

	private fun checkInMainThread(lazyMsg: () -> Any) {
		check(Looper.myLooper() == Looper.getMainLooper(), lazyMsg)
	}

	companion object {
		private val MAX_PROGRESS_COLLECTION_INTERVAL: Duration = 1.seconds
		private val MIN_PROGRESS_COLLECTION_INTERVAL: Duration = 100.milliseconds
	}
}
