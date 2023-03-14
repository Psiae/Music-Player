package com.flammky.musicplayer.playbackcontrol.presentation.r

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.PlaybackSessionConnector
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import com.flammky.musicplayer.player.presentation.r.RealPlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class RealPlaybackProgressionCollector(
    private val user: User,
    private val observer: RealPlaybackObserver,
    private val scope: CoroutineScope,
	// remove
    private val playbackConnection: PlaybackConnection,
    collectEvent: Boolean,
) : PlaybackObserver.ProgressionCollector {

	private val _stateLock = Any()

	private var progressCollectorJob: Job? = null
		set(value) {
			field = value
		}

	private var eventCollectorJob: Job? = null
		set(value) {
			field = value
		}

	private var _collectEvent: Boolean = collectEvent

	private var _intervalHandler: (
		isEvent: Boolean,
		progress: Duration,
		bufferedProgress: Duration,
		duration: Duration,
		speed: Float
	) -> Duration? = { _, _, _, _, _ ->
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
			if (collectEvent) playbackConnection.requestUserSessionAsync(user).await().controller
				.withLooperContext {
					_progressStateFlow.value = getPosition()
					_bufferedProgressStateFlow.value = getBufferedPosition()
				}
		}
		if (collectEvent) {
			startCollectEvent()
		}
	}

	override fun startCollectPosition(): Job {
		val start = scope.launch {
			if (progressCollectorJob?.isActive == true) {
				return@launch
			}
			_progressStateFlow.value = null
			_bufferedProgressStateFlow.value = null
			internalStartProgress(false)
			startCollectEvent()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			start.join()
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
		handler: (
			isEvent: Boolean,
			progress: Duration,
			bufferedProgress: Duration,
			duration: Duration,
			speed: Float
		) -> Duration?
	) {
		scope.launch {
			_intervalHandler = handler
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
		session: PlaybackSessionConnector? = null
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
		session: PlaybackSessionConnector? = null
	): Job {
		return scope.launch {
			if (isEvent) {
				var progress: Duration = PlaybackConstants.POSITION_UNSET
				var buffered: Duration = PlaybackConstants.POSITION_UNSET
				var duration: Duration = PlaybackConstants.DURATION_UNSET
				var speed: Float = 1f

				session?.controller?.withLooperContext {
					progress = getPosition()
					buffered = getBufferedPosition()
					duration = getDuration()
					speed = getPlaybackSpeed()
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				val nextInterval = _intervalHandler(true, progress, buffered, duration, speed)
					?: run {
						val currentProgress = session?.controller?.withLooperContext { progress }
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
				val currentSession = session ?: playbackConnection.requestUserSessionAsync(user).await()
				var progress: Duration = PlaybackConstants.POSITION_UNSET
				var buffered: Duration = PlaybackConstants.POSITION_UNSET
				var duration: Duration = PlaybackConstants.DURATION_UNSET
				var speed: Float = 1f

				currentSession.controller.withLooperContext {
					progress = getPosition()
					buffered = getBufferedPosition()
					duration = getDuration()
					speed = getPlaybackSpeed()
				}

				_bufferedProgressStateFlow.update { buffered }
				_progressStateFlow.update { progress }

				nextInterval = _intervalHandler(false, progress, buffered, duration, speed)
					?: run {
						val currentProgress = session?.controller?.withLooperContext { progress }
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
			val session = playbackConnection.requestUserSessionAsync(user).await()
			session.controller
				.apply {
					val observer = acquireObserver(owner)
					launch {
						observer.observeIsPlaying().collect { playing ->
							if (playing && progressCollectorJob?.isActive != true) {
								internalStartProgress(true, session)
							} else if (!playing && progressCollectorJob?.isActive == true) {

								stopCollectProgressInternal()
								var buffered: Duration = PlaybackConstants.POSITION_UNSET
								var progress: Duration = PlaybackConstants.POSITION_UNSET

								session.controller.withLooperContext {
									buffered = getBufferedPosition()
									progress = getPosition()
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
					}
					launch {
						observer.observePositionDiscontinuityEvent().collect {
							if (session.controller.withLooperContext { isPlaying() }) {
								internalStartProgress(true, session)
							} else {
								var buffered: Duration = PlaybackConstants.POSITION_UNSET
								var progress: Duration = PlaybackConstants.POSITION_UNSET

								session.controller.withLooperContext controller@ {
									buffered = getBufferedPosition()
									progress = getPosition()
								}

								_bufferedProgressStateFlow.update { buffered }
								_progressStateFlow.update { progress }
							}
						}
					}
					runCatching { awaitCancellation() }.onFailure { session.controller.releaseObserver(owner) }
				}
		}
	}
}
