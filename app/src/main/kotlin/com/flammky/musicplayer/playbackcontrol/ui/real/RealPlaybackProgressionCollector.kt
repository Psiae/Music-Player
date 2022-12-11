package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
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
) : PlaybackObserver.ProgressionCollector {

	private var progressCollectorJob: Job? = null

	private var eventCollectorJob: Job? = null

	private var collectingEvent: Boolean = false

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
			if (collectingEvent) playbackConnection.getSession()?.controller?.withContext {
				_progressStateFlow.value = progress
				_bufferedProgressStateFlow.value = bufferedProgress
			}
		}
		if (collectEvent) {
			startCollectEvent()
		}
	}

	override fun startCollectProgress(): Job {
		scope.launch {
			startCollectProgress(false)
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			_progressStateFlow.first { it != null }
			_bufferedProgressStateFlow.first { it != null }
		}
	}

	override fun stopCollectProgress(): Job {
		return scope.launch { stopCollectProgressInternal() }
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

	override fun setCollectEvent(collectEvent: Boolean): Job {
		return scope.launch { setCollectEventInternal(collectEvent) }
	}

	override fun dispose() {
		scope.cancel()
		observer.notifyCollectorDisposed(this)
	}

	private suspend fun setCollectEventInternal(collectEvent: Boolean) {
		if (collectEvent && !collectingEvent) {
			startCollectEvent()
		} else if (!collectEvent && collectingEvent) {
			stopCollectEvent()
		}
	}

	private suspend fun startCollectProgress(
		isEvent: Boolean
	) {
		stopCollectProgressInternal()
		progressCollectorJob = dispatchCollectProgress(isEvent)
	}

	private suspend fun stopCollectProgressInternal() {
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

				playbackConnection.getSession()?.controller?.withContext {
					progress = this.progress
					buffered = bufferedProgress
					duration = this.duration
					speed = playbackSpeed
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				nextInterval = intervalHandler(isEvent, progress, duration, speed)
					?: run {
						val fromNextSecond = (1010 - (playbackConnection.getSession()?.controller?.withContext { progress }?.inWholeMilliseconds ?: 0) % 1000)
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

		this.collectingEvent = true

		eventCollectorJob = dispatchCollectEvent()
	}

	private fun stopCollectEvent() {
		if (!this.collectingEvent) {
			return
		}

		this.collectingEvent = false

		eventCollectorJob?.cancel()
	}

	private fun dispatchCollectEvent(): Job {
		return scope.launch {

			val isPlayingCollector = scope.launch {

				val owner = Any()
				callbackFlow {
					var controller: PlaybackController? = null
					playbackConnection.observeCurrentSession().distinctUntilChanged().collect {
						controller?.releaseObserver(owner)
						controller = it?.controller
						controller?.withContext {
							acquireObserver(owner).getAndObserveIsPlayingChange {
								scope.launch { send(it.new) }
							}
						}?.let { send(it) }
					}
					awaitClose { controller?.releaseObserver(owner) }
				}.collect {

					check(collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}
					if (it && progressCollectorJob?.isActive != true) {
						startCollectProgress(true)
					} else if (!it && progressCollectorJob?.isActive == true) {
						stopCollectProgressInternal()
						var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
						var progress: Duration = PlaybackConstants.PROGRESS_UNSET

						playbackConnection.getSession()?.controller?.withContext {
							buffered = this.bufferedProgress
							progress = this.progress
						}

						_bufferedProgressStateFlow.update { buffered }
						_progressStateFlow.update { progress }
					}
				}
			}

			val progressDiscontinuityCollector = scope.launch {

				val owner = Any()
				callbackFlow {
					var controller: PlaybackController? = null
					playbackConnection.observeCurrentSession().distinctUntilChanged().collect {
						controller?.releaseObserver(owner)
						controller = it?.controller
						controller?.withContext {
							acquireObserver(owner).getAndObserveIsPlayingChange {
								scope.launch { send(it.new) }
							}
						}?.let { send(it) }
					}
					awaitClose { controller?.releaseObserver(owner) }
				}.collect {

					check(collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}

					if (playbackConnection.getSession()?.controller?.withContext { playing } == true) {
						startCollectProgress(true)
					} else {
						var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
						var progress: Duration = PlaybackConstants.PROGRESS_UNSET

						playbackConnection.getSession()?.controller?.withContext {
							buffered = this.bufferedProgress
							progress = this.progress
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
