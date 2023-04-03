package com.flammky.musicplayer.playbackcontrol.presentation.r

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import com.flammky.musicplayer.player.presentation.r.RealPlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class RealPlaybackQueueCollector(
    private val user: User,
    private val observer: RealPlaybackObserver,
    private val scope: CoroutineScope,
    private val playbackConnection: PlaybackConnection
) : PlaybackObserver.QueueCollector {

	private val _lock = Any()

	private val localUNSET = OldPlaybackQueue.UNSET.copy()

	private val _queueStateFlow = MutableStateFlow(localUNSET)

	override val queueStateFlow: StateFlow<OldPlaybackQueue> =
		_queueStateFlow.mapLatest { if (it === localUNSET) PlaybackConstants.QUEUE_UNSET else it }
			.stateIn(scope, SharingStarted.Lazily, PlaybackConstants.QUEUE_UNSET)


	private var queueCollectorJob: Job? = null

	@GuardedBy("_lock")
	override var disposed: Boolean = false
		get() = sync(_lock) { field }
		private set(value) = sync(_lock) { field = value }

	init {
		val scopeDispatcher = scope.coroutineContext[CoroutineDispatcher]
		require(scopeDispatcher?.limitedParallelism(1) === scopeDispatcher) {
			"CoroutineScope dispatcher was not confined"
		}
	}

	fun updateQueue(): Job {
		return scope.launch {
			val s = playbackConnection.requestUserSessionAsync(user).await()
			_queueStateFlow.update {
				(s.controller.getQueue()
					.takeIf { it != OldPlaybackQueue.UNSET } ?: localUNSET).also { new ->
					Timber.d(
						"""
						QueueCollector updateQueue
							from $it
							to $new
						"""
					)
				}
			}
		}
	}

	override fun startCollect(): Job {
		val launch = scope.launch {
			startObserveInternal()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			launch.join()
			_queueStateFlow.first { it !== localUNSET }
		}
	}

	override fun stopCollect(): Job {
		return scope.launch {
			queueCollectorJob?.cancel()
		}
	}

	override fun dispose() {
		sync(_lock) {
			if (disposed) return
			scope.cancel()
			disposed = true
		}
		observer.notifyCollectorDisposed(this)
	}

	// keep it suspend as it should be called via the confined context only
	private suspend fun startObserveInternal() {
		if (queueCollectorJob?.isActive == true) {
			return
		}
		_queueStateFlow.emit(localUNSET)
		queueCollectorJob = scope.launch {
			val owner = Any()
			playbackConnection.requestUserSessionAsync(user).await().controller
				.apply {
					runCatching {
						acquireObserver(owner).observeQueue().collect {
							_queueStateFlow.value = it
						}
					}.onFailure {
						releaseObserver(owner)
					}
				}
		}
	}
}
