package com.flammky.musicplayer.playbackcontrol.ui.r

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackEvent
import com.flammky.musicplayer.media.playback.PlaybackSession
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class RealPlaybackProgressionCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	// remove
	private val playbackConnection: PlaybackConnection,
	collectEvent: Boolean,
) : PlaybackObserver.ProgressionCollector {

	private val _stateLock = Any()

	private var progressCollectorJob: Job? = null
		set(value) {
			require(value == null || scope.coroutineContext.job.children.contains(value)) {
				"Job=$value is not attached to Scope=$scope"
			}
			field = value
		}

	private var eventCollectorJob: Job? = null
		set(value) {
			require(value == null || scope.coroutineContext.job.children.contains(value)) {
				"Job=$value is not attached to Scope=$scope"
			}
			field = value
		}

	private var _collectEvent: Boolean = collectEvent

	private var intervalHandler: suspend (
		isEvent: Boolean, progress: Duration, duration: Duration, speed: Float
	) -> Duration? = { _, _, _, _ ->
		null
	}

	private val _progressStateFlow = MutableStateFlow<Duration?>(null)
	private val _bufferedProgressStateFlow = MutableStateFlow<Duration?>(null)

	@GuardedBy("lock")
	override var disposed: Boolean = false
		get() = sync(_stateLock) { field }
		private set(value) {
			check(Thread.holdsLock(_stateLock))
			field = value
		}

	override val positionStateFlow: StateFlow<Duration> = _progressStateFlow
		.mapLatest { it ?: PlaybackConstants.POSITION_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.POSITION_UNSET)

	override val bufferedPositionStateFlow: StateFlow<Duration> = _bufferedProgressStateFlow
		.mapLatest { it ?: PlaybackConstants.POSITION_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.POSITION_UNSET)

	init {
		observer.observeUpdateRequest(scope) {
			if (collectEvent) playbackConnection.getSession()?.controller?.withContext {
				_progressStateFlow.value = progress
				_bufferedProgressStateFlow.value = bufferedProgress
			}
		}
		if (collectEvent) {
			startCollectEvent()
		}
	}

	override fun startCollectPosition(): Job {
		scope.launch {
			if (progressCollectorJob?.isActive == true) {
				return@launch
			}
			_progressStateFlow.value = null
			_bufferedProgressStateFlow.value = null
			internalStartProgress(false)
			startCollectEvent()
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
		Timber.d("ProgressionCollector $this dispose")
		sync(_stateLock) {
			if (disposed) return
			scope.cancel()
			disposed = true
		}
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
		isEvent: Boolean,
		session: PlaybackSession? = null
	) {
		stopCollectProgressInternal()
		progressCollectorJob = dispatchCollectProgress(isEvent, session)
	}

	private suspend fun stopCollectProgressInternal() {
		coroutineContext.ensureActive()
		progressCollectorJob?.cancel()
	}

	private fun dispatchCollectProgress(
		isEvent: Boolean,
		session: PlaybackSession? = null
	): Job {
		return scope.launch {
			if (isEvent) {
				var progress: Duration = PlaybackConstants.POSITION_UNSET
				var buffered: Duration = PlaybackConstants.POSITION_UNSET
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

				val nextInterval = intervalHandler(true, progress, duration, speed)
					?: run {
						val currentProgress = session?.controller?.withContext { progress }
							?.takeIf { it != PlaybackConstants.POSITION_UNSET }
							?: return@run PlaybackConstants.POSITION_UNSET
						// from next second
						(1010 - currentProgress.inWholeMilliseconds % 1000 / speed).toLong().milliseconds
					}

				if (nextInterval < Duration.ZERO) {
					// should be handled by other callback
					return@launch
				}

				delay(nextInterval)
			}

			var nextInterval: Duration
			do {
				val currentSession = session ?: playbackConnection.getSession()
				var progress: Duration = PlaybackConstants.POSITION_UNSET
				var buffered: Duration = PlaybackConstants.POSITION_UNSET
				var duration: Duration = PlaybackConstants.DURATION_UNSET
				var speed: Float = 1f

				currentSession?.controller?.withContext {
					progress = this.progress
					buffered = bufferedProgress
					duration = this.duration
					speed = playbackSpeed
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				nextInterval = intervalHandler(false, progress, duration, speed)
					?: run {
						val currentProgress = session?.controller?.withContext { progress }
							?.takeIf { it != PlaybackConstants.POSITION_UNSET }
							?: return@run PlaybackConstants.POSITION_UNSET
						// from next second
						((1010 - currentProgress.inWholeMilliseconds % 1000) / speed).toLong().milliseconds
					}

				if (nextInterval < Duration.ZERO) {
					// should be handled by other callback
					break
				}

				delay(nextInterval)
			} while (coroutineContext.isActive)
		}
	}

	private fun startCollectEvent() {
		if (!_collectEvent || eventCollectorJob?.isActive == true) {
			return
		}
		eventCollectorJob = dispatchCollectEvent()
	}

	private fun stopCollectEvent() {
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
					check(_collectEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}
					// cancel any active listener
					listenerJob?.cancel()
					if (session == null) {
						listenerJob = null
						stopCollectProgressInternal()
						_bufferedProgressStateFlow.update { PlaybackConstants.POSITION_UNSET }
						_progressStateFlow.update { PlaybackConstants.POSITION_UNSET }
						return@transform
					}
					// as emitting from different coroutine context is not allowed, emit outside the listener
					// job via this channel
					val channel = Channel<Pair<PlaybackSession, PlaybackEvent>>(1)
					listenerJob = scope.launch {
						try {
							session.controller.acquireObserver(owner).let { observer ->
								observer.observeDiscontinuity {
									channel.trySend(session to it)
								}
								observer.getAndObserveIsPlayingChange {
									channel.trySend(session to it)
								}.also { playing ->
									// current
									if (playing && progressCollectorJob?.isActive != true) {
										internalStartProgress(true, session)
									} else if (!playing && progressCollectorJob?.isActive == true) {

										stopCollectProgressInternal()
										var buffered: Duration = PlaybackConstants.POSITION_UNSET
										var progress: Duration = PlaybackConstants.POSITION_UNSET

										session.controller.withContext {
											buffered = this@withContext.bufferedProgress
											progress = this@withContext.progress
										}

										_bufferedProgressStateFlow.update { buffered }
										_progressStateFlow.update { progress }
									}
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
				.collect { sessionEvent ->
					// assert the collectEvent state
					check(_collectEvent) {
						"CollectEventJob was still active when `collectingEvent` is false"
					}
					@Suppress("UnnecessaryVariable")
					val session = sessionEvent.first
					@Suppress("UnnecessaryVariable")
					val event = sessionEvent.second
					when (event) {
						// isPlaying
						// playWhenReady
						is PlaybackEvent.IsPlayingChange -> {
							val playing = event.new
							if (playing && progressCollectorJob?.isActive != true) {
								internalStartProgress(true, session)
							} else if (!playing && progressCollectorJob?.isActive == true) {

								stopCollectProgressInternal()
								var buffered: Duration = PlaybackConstants.POSITION_UNSET
								var progress: Duration = PlaybackConstants.POSITION_UNSET

								session.controller.withContext controller@ {
									buffered = this@controller.bufferedProgress
									progress = this@controller.progress
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
						// Discontinuity
						is PlaybackEvent.ProgressDiscontinuity -> {
							if (session.controller.withContext { playing }) {
								internalStartProgress(true, session)
							} else {
								var buffered: Duration = PlaybackConstants.POSITION_UNSET
								var progress: Duration = PlaybackConstants.POSITION_UNSET

								session.controller.withContext controller@ {
									buffered = this@controller.bufferedProgress
									progress = this@controller.progress
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
						else -> error("Unused PlaybackEvent is sent to collector")
					}
				}
		}
	}
}
