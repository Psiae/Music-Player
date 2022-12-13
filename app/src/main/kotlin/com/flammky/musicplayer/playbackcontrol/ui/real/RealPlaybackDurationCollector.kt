package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

internal class RealPlaybackDurationCollector(
	private val parentObserver: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver.DurationCollector {
	// unfortunately identity check is not allowed
	private val _durationStateFlow = MutableStateFlow<Duration?>(null)

	private var _job: Job? = null

	@OptIn(ExperimentalCoroutinesApi::class)
	override val durationStateFlow: StateFlow<Duration> = _durationStateFlow
		.mapLatest { it ?: PlaybackConstants.DURATION_UNSET }
		.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.DURATION_UNSET)

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
		// as the session is observed within the scope, this is enough
		scope.cancel()
		parentObserver.notifyCollectorDisposed(this)
	}

	private fun collectDuration(): Job {
		return scope.launch {
			val owner = Any()
			var listenerJob: Job? = null
			playbackConnection.observeCurrentSession().distinctUntilChanged()
				.transform { session ->
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
				.onCompletion {
					listenerJob?.cancel()
				}
				.collect(_durationStateFlow)
		}
	}
}
