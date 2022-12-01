package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackProgressionCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class RealPlaybackProgressionCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection,
	collectEvent: Boolean,
) : PlaybackProgressionCollector {

	private var progressCollectorJob: Job? = null

	private var eventCollectorJob: Job? = null

	private var collectingEvent: Boolean = collectEvent

	private var intervalHandler: suspend (
		isEvent: Boolean, progress: Duration, duration: Duration, speed: Float
	) -> Duration? = { _, _, _, _ ->
		null
	}

	private val _progressStateFlow = MutableStateFlow<Duration?>(null)
	private val _bufferedProgressStateFlow = MutableStateFlow<Duration?>(null)

	override val progressStateFlow: StateFlow<Duration> = _progressStateFlow
		.mapLatest { it ?: PlaybackConstants.PROGRESS_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.PROGRESS_UNSET)

	override val bufferedProgressStateFlow: StateFlow<Duration> = _bufferedProgressStateFlow
		.mapLatest { it ?: PlaybackConstants.PROGRESS_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.PROGRESS_UNSET)

	init {
		observer.observeUpdateRequest(scope) {
			if (collectingEvent) playbackConnection.joinContext {
				_progressStateFlow.value = getProgress()
				_bufferedProgressStateFlow.value = getBufferedProgress()
			}
		}
	}

	override fun startCollectProgressAsync(): Deferred<Unit> {
		scope.launch {
			startCollectProgress(false)
		}
		return scope.async(start = CoroutineStart.LAZY) {
			_progressStateFlow.first { it != null }
			_bufferedProgressStateFlow.first { it != null }
		}
	}

	override fun stopCollectProgressAsync(): Deferred<Unit> {
		return scope.async {
			stopCollectProgress()
		}
	}

	override fun setIntervalHandler(
		handler: suspend (isEvent: Boolean, progress: Duration, duration: Duration, speed: Float) -> Duration?
	) {
		scope.launch {
			intervalHandler = handler
			if (progressCollectorJob?.isActive == true) {
				startCollectProgress(false)
			}
		}
	}

	override fun setCollectEventAsync(collectEvent: Boolean): Deferred<Unit> {
		return scope.async {
			if (collectEvent && !collectingEvent) {
				startCollectEvent()
			} else if (!collectEvent && collectingEvent) {
				stopCollectEvent()
			}
		}
	}

	override fun dispose() {
		scope.cancel()
	}

	private suspend fun startCollectProgress(
		isEvent: Boolean
	) {
		stopCollectProgress()
		progressCollectorJob = dispatchCollectProgress(isEvent)
	}

	private suspend fun stopCollectProgress() {
		coroutineContext.ensureActive()
		progressCollectorJob?.cancel()
	}

	private fun dispatchCollectProgress(
		isEvent: Boolean
	): Job {
		return scope.launch {
			var nextInterval: Duration
			do {
				var progress: Duration = PlaybackConstants.PROGRESS_UNSET
				var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
				var duration: Duration = PlaybackConstants.DURATION_UNSET
				var speed: Float = 1f

				playbackConnection.joinContext {
					progress = getProgress()
					buffered = getBufferedProgress()
					speed = getPlaybackSpeed()
					duration = getDuration()
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				nextInterval = intervalHandler(isEvent, progress, duration, speed)
					?: run {
						val fromNextSecond = (1010 - playbackConnection.getProgress().inWholeMilliseconds % 1000)
						(fromNextSecond / speed).toLong()
					}.milliseconds

				if (nextInterval < Duration.ZERO) {
					// should be handled by other callback
					break
				}

				delay(nextInterval)
			} while (coroutineContext.isActive)
		}
	}

	private fun startCollectEvent() {
		if (this.collectingEvent) {
			return
		}

		eventCollectorJob = dispatchCollectEvent()

		this.collectingEvent = true
	}

	private fun stopCollectEvent() {
		if (!this.collectingEvent) {
			return
		}

		eventCollectorJob?.cancel()

		this.collectingEvent = false
	}

	private fun dispatchCollectEvent(): Job {
		return scope.launch {

			val isPlayingCollector = scope.launch {
				playbackConnection.observeIsPlaying().collect {

					check(collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}

					if (it && progressCollectorJob?.isActive != true) {
						startCollectProgress(true)
					} else if (!it && progressCollectorJob?.isActive == true) {
						stopCollectProgress()
					}
				}
			}

			val progressDiscontinuityCollector = scope.launch {
				playbackConnection.observeProgressDiscontinuity().collect {

					check(collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}

					if (playbackConnection.getIsPlaying()) {
						startCollectProgress(true)
					} else {
						var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
						var progress: Duration = PlaybackConstants.PROGRESS_UNSET

						playbackConnection.joinContext {
							buffered = getBufferedProgress()
							progress = getProgress()
						}

						_bufferedProgressStateFlow.update { buffered }
						_progressStateFlow.update { progress }
					}
				}
			}
			isPlayingCollector.join()
			progressDiscontinuityCollector.join()
		}
	}
}
