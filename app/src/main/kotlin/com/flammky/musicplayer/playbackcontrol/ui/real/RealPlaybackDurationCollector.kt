package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.time.Duration

class RealPlaybackDurationCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver.DurationCollector {

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
		val launch = scope.launch {
			if (_job?.isActive != true) {
				return@launch
			}
			_job?.cancel()
				?: error("Concurrency Error")
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			launch.join()
		}
	}

	override fun dispose() {
		// as the session is observed within the scope, this is enough
		scope.cancel()
		observer.notifyCollectorDisposed(this)
	}

	@OptIn(ExperimentalStdlibApi::class)
	private fun collectDuration(): Job {
		return scope.launch {
			val owner = Any()
			var job: Job? = null
			playbackConnection.observeCurrentSession().distinctUntilChanged().collect { session ->
				job?.cancel()
				if (session == null) {
					return@collect
				}
				job = launch {
					val observer = session.controller.acquireObserver(owner)
					_durationStateFlow.value = observer.getAndObserveDurationChange { new ->
						_durationStateFlow.value = new
					}
					try {
						awaitCancellation()
					} finally {
						Timber.d("DEBUG_DurationCollector: release observer")
						session.controller.releaseObserver(owner)
					}
				}
			}
		}
	}
}
