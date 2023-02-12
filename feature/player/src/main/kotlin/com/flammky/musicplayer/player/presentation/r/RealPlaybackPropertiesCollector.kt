package com.flammky.musicplayer.playbackcontrol.presentation.r

import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import com.flammky.musicplayer.player.presentation.r.RealPlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.annotation.concurrent.GuardedBy

internal class RealPlaybackPropertiesCollector(
    private val user: User,
    private val scope: CoroutineScope,
    private val parentObserver: RealPlaybackObserver,
    private val connection: PlaybackConnection
) : PlaybackObserver.PropertiesCollector {

	private val _lock = Any()
	private var _disposed = false

	@Volatile
	@GuardedBy("scope") // only mutable by `scope`
	private var _job: Job = Job().apply { cancel() }
		set(value) {
			require(scope.coroutineContext.job.children.contains(value)) {
				"Job=$value is not attached to Scope=$scope"
			}
			field = value
		}

	private val _stateFlow = MutableStateFlow<PlaybackProperties>(LOCAL_PROPERTIES_UNSET)

	override val disposed: Boolean
		get() = sync(_lock) { _disposed }

	override val propertiesStateFlow: StateFlow<PlaybackProperties> = _stateFlow.asStateFlow()

	override fun dispose() {
		sync(_lock) {
			if (_disposed) return
			// will cancel _job
			scope.cancel()
			_disposed = true
		}
		parentObserver.notifyCollectorDisposed(this)
	}

	override fun startCollect(): Job {
		val launch = scope.launch {
			if (_job.isActive) {
				return@launch
			}
			_stateFlow.value = LOCAL_PROPERTIES_UNSET
			_job = collectProperties()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			launch.join()
			propertiesStateFlow.first { it !== LOCAL_PROPERTIES_UNSET }
		}
	}

	override fun stopCollect(): Job {
		return scope.launch() {
			_job.cancel()
		}
	}

	private fun collectProperties(): Job {
		return scope.launch {
			val owner = Any()
			connection.requestUserSessionAsync(user).await().controller
				.apply {
					acquireObserver(owner).observePlaybackProperties()
						.runCatching {
							collect { _stateFlow.value = it }
						}.onFailure {
							releaseObserver(owner)
						}
				}
		}
	}

	fun updatePlayWhenReady(): Job {
		return scope.launch {
			val c = connection.requestUserSessionAsync(user).await().controller
			_stateFlow.value = c.withLooperContext { getPlaybackProperties() }
		}
	}

	fun updateRepeatMode(): Job {
		return scope.launch {
			val c = connection.requestUserSessionAsync(user).await().controller
			_stateFlow.value = c.withLooperContext { getPlaybackProperties() }
		}
	}

	fun updateShuffleMode(): Job {
		return scope.launch {
			val c = connection.requestUserSessionAsync(user).await().controller
			_stateFlow.value = c.withLooperContext { getPlaybackProperties() }
		}
	}

	companion object {
		private val LOCAL_PROPERTIES_UNSET = PlaybackConstants.PROPERTIES_UNSET
	}
}
