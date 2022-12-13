package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackEvent
import com.flammky.musicplayer.media.playback.PlaybackSession
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class RealPlaybackProgressionCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection,
	collectEvent: Boolean,
) : PlaybackObserver.ProgressionCollector {

	private var progressCollectorJob: Job? = null

	private var eventCollectorJob: Job? = null

	private var _collectingEvent: Boolean = false

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
			if (_collectingEvent) playbackConnection.getSession()?.controller?.withContext {
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
			if (progressCollectorJob?.isActive == true) {
				return@launch
			}
			_progressStateFlow.value = null
			_bufferedProgressStateFlow.value = null

			if (_collectingEvent) {
				startCollectEvent()
			} else {
				internalStartProgress(false)
			}
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			_progressStateFlow.first { it != null }
			_bufferedProgressStateFlow.first { it != null }
		}
	}

	override fun stopCollectProgress(): Job {
		return scope.launch {
			stopCollectProgressInternal()
			stopCollectEvent()
		}
	}

	override fun setIntervalHandler(
		handler: suspend (isEvent: Boolean, progress: Duration, duration: Duration, speed: Float) -> Duration?
	) {
		scope.launch {
			intervalHandler = handler
			if (progressCollectorJob?.isActive == true) {
				internalStartProgress(false)
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
		if (collectEvent) {
			startCollectEvent()
		} else  {
			stopCollectEvent()
		}
	}

	private suspend fun internalStartProgress(
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
				val session = playbackConnection.getSession()
				var progress: Duration = PlaybackConstants.PROGRESS_UNSET
				var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
				var duration: Duration = PlaybackConstants.DURATION_UNSET
				var speed: Float = 1f

				session?.controller?.withContext {
					progress = this.progress
					buffered = bufferedProgress
					duration = this.duration
					speed = playbackSpeed
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				nextInterval = intervalHandler(isEvent, progress, duration, speed)
					?: run {
						val fromNextSecond = (1010 - (session?.controller?.withContext { progress }?.inWholeMilliseconds ?: 0) % 1000)
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
		if (_collectingEvent || eventCollectorJob?.isActive == true) {
			return
		}
		_collectingEvent = true
		eventCollectorJob = dispatchCollectEvent()
	}

	private fun stopCollectEvent() {
		_collectingEvent = false
		eventCollectorJob?.cancel()
	}

	private fun dispatchCollectEvent(): Job {
		return scope.launch {

			val owner = Any()
			var listenerJob: Job? = null
			// TODO: observe session should be done separately
			playbackConnection.observeCurrentSession().distinctUntilChanged()
				.transform { session ->
					// assert the collectEvent state
					check(_collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}
					// cancel any active listener
					listenerJob?.cancel()
					if (session == null) {
						// there is no current session, emit UNSET value
						emit(null to false)
						return@transform
					}
					// as emitting from different coroutine context is not allowed, emit outside the listener
					// job via this channel
					val channel = Channel<Pair<PlaybackSession?, Any>>(1)
					listenerJob = launch {
						try {
							session.controller.acquireObserver(owner).let { observer ->
								observer.observeDiscontinuity {
									channel.trySend(session to it)
								}
								observer.getAndObserveIsPlayingChange {
									channel.trySend(session to it)
								}.also { playing ->
									// send current
									channel.send(session to playing)
								}
							}
							// wait until job cancellation, cleanup will be done inside finally block
							awaitCancellation()
						} finally {
							// not sure if closing the channel is necessary as the emitter is already cancelled
							channel.close()
							session.controller.releaseObserver(owner)
						}
					}
					// emit the channel until job cancellation
					emitAll(channel.consumeAsFlow())
				}
				.onCompletion {
					// the flow collector is completed, cancel the current listener Job and let it cleanup
					listenerJob?.cancel()
				}
				// should we collect all of them here, or launch in above collect block ?
				// the latter would provide clearer event interest
				.collect { pair ->

					// assert the collectEvent state
					check(_collectingEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}
					@Suppress("UnnecessaryVariable")
					val session = pair.first
					@Suppress("UnnecessaryVariable")
					val sent = pair.second
					val transformed = when (sent) {
						is PlaybackEvent.IsPlayingChange -> sent.new
						else -> sent
					}
					when (transformed) {
						// isPlaying
						is Boolean -> {
							@Suppress("UnnecessaryVariable")
							val playing = transformed
							if (playing && progressCollectorJob?.isActive != true) {
								internalStartProgress(true)
							} else if (!playing && progressCollectorJob?.isActive == true) {
								stopCollectProgressInternal()
								var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
								var progress: Duration = PlaybackConstants.PROGRESS_UNSET

								session?.controller?.withContext {
									buffered = this@withContext.bufferedProgress
									progress = this@withContext.progress
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
						// Discontinuity
						is PlaybackEvent.ProgressDiscontinuity -> {
							if (session?.controller?.withContext { playing } == true) {
								internalStartProgress(true)
							} else {
								var buffered: Duration = PlaybackConstants.PROGRESS_UNSET
								var progress: Duration = PlaybackConstants.PROGRESS_UNSET

								session?.controller?.withContext {
									buffered = this.bufferedProgress
									progress = this.progress
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
					}
				}
		}
	}
}
