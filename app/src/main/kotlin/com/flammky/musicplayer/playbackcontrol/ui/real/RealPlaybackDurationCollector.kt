package com.flammky.musicplayer.playbackcontrol.ui.real

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.time.Duration

internal class RealPlaybackDurationCollector(
	private val parentObserver: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver.DurationCollector {
	// unfortunately identity check is not allowed
	private val _durationStateFlow = MutableStateFlow<Duration?>(null)
	private val _lock = Any()

	@Volatile
	private var _job: Job? = null

	@GuardedBy("lock")
	override var disposed: Boolean = false
		get() = sync(_lock) { field }
		private set(value) {
			check(Thread.holdsLock(_lock))
			field = value
		}

	@OptIn(ExperimentalCoroutinesApi::class)
	override val durationStateFlow: StateFlow<Duration> = _durationStateFlow
		.mapLatest { it ?: PlaybackConstants.DURATION_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.DURATION_UNSET)

	init {
		Timber.d("DurationCollector init")
	}

	override fun startCollect(): Job {
		val launch = scope.launch {
			if (_job?.isActive == true) {
				return@launch
			}
			_durationStateFlow.value = null
			_job = collectDuration()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			launch.join()
			_durationStateFlow.first { it != null }
		}
	}

	override fun stopObserve(): Job {
		return scope.launch() {
			if (_job?.isActive != true) {
				return@launch
			}
			_job?.cancel()
				?: error("Concurrency Error")
		}
	}

	override fun dispose() {
		Timber.d("DurationCollector $this dispose")
		sync(_lock) {
			if (disposed) return
			scope.cancel()
			disposed = true
		}
		parentObserver.notifyCollectorDisposed(this)
	}

	private fun collectDuration(): Job {
		return scope.launch {
			Timber.d("DurationCollector collectDuration launched")
			val owner = Any()
			var listenerJob: Job? = null
			playbackConnection.observeCurrentSession().distinctUntilChanged()
				.transform { session ->
					Timber.d("DurationCollector got session $session")
					listenerJob?.cancel()
					if (session == null) {
						emit(PlaybackConstants.DURATION_UNSET)
						return@transform
					}
					val channel = Channel<Duration>(1)
					listenerJob = launch {
						try {
							session.controller.acquireObserver(owner).let { observer ->
								val get = observer.getAndObserveDurationChange { new ->
									channel.trySend(new)
								}
								channel.send(get)
							}
							awaitCancellation()
						} finally {
							channel.close()
							session.controller.releaseObserver(owner)
						}
					}
					emitAll(channel.consumeAsFlow())
				}
				.onEach {
					Timber.d("DurationCollector collected $it")
				}
				.collect(_durationStateFlow)
		}
	}
}
