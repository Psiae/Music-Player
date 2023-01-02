package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackProperties
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.annotation.concurrent.GuardedBy
import com.flammky.musicplayer.media.playback.PlaybackController as ConnectionController

internal class RealPlaybackPropertiesCollector(
	private val scope: CoroutineScope,
	private val parentObserver: RealPlaybackObserver,
	private val connectionController: ConnectionController
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
			val channel = Channel<PlaybackProperties>(capacity = Channel.UNLIMITED)
			connectionController.acquireObserver(owner).getAndObservePropertiesChange {
				channel.trySend(it)
			}.also {
				channel.send(it)
			}
			try {
				channel.consumeAsFlow().collect {
					_stateFlow.value = it
				}
			} finally {
				connectionController.releaseObserver(owner)
				channel.close()
			}
		}
	}

	fun updatePlayWhenReady(): Job {
		return scope.launch {
			_stateFlow.update {
				connectionController.getPlaybackProperties()
			}
		}
	}

	fun updateRepeatMode(): Job {
		return scope.launch {
			_stateFlow.update {
				connectionController.getPlaybackProperties()
			}
		}
	}

	fun updateShuffleMode(): Job {
		return scope.launch {
			_stateFlow.update {
				connectionController.getPlaybackProperties()
			}
		}
	}

	companion object {
		private val LOCAL_PROPERTIES_UNSET = PlaybackConstants.PROPERTIES_UNSET
	}
}
