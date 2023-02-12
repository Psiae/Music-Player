package com.flammky.musicplayer.playbackcontrol.presentation.r

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import com.flammky.musicplayer.player.presentation.r.RealPlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.time.Duration

internal class RealPlaybackDurationCollector(
    private val user: User,
	// parent should provide the necessary function
    private val parentObserver: RealPlaybackObserver,
    private val scope: CoroutineScope,
    private val connection: PlaybackConnection
) : PlaybackObserver.DurationCollector {
	// unfortunately identity check is not allowed
	private val _durationStateFlow = MutableStateFlow<Duration?>(null)
	private val _lock = Any()

	@Volatile
	private var _job: Job? = null
		set(value) {
			require(value == null || scope.coroutineContext.job.children.contains(value)) {
				"Job=$value is not attached to Scope=$scope"
			}
			field = value
		}

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

	override fun stopCollect(): Job {
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
			val owner = Any()
			connection.requestUserSessionAsync(user).await().controller
				.apply {
					acquireObserver(owner).observeDuration()
						.runCatching {
							collect {
								_durationStateFlow.value = it
							}
						}
						.onFailure {
							releaseObserver(owner)
						}
				}
		}
	}
}
