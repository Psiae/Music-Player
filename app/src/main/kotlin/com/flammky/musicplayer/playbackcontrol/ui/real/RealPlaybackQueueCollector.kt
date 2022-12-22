package com.flammky.musicplayer.playbackcontrol.ui.real

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class RealPlaybackQueueCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver.QueueCollector {

	private val _lock = Any()

	private val localUNSET = PlaybackQueue.UNSET.copy()

	private val _queueStateFlow = MutableStateFlow(localUNSET)

	override val queueStateFlow: StateFlow<PlaybackQueue> = _queueStateFlow.asStateFlow()

	private var queueCollectorJob: Job? = null

	@GuardedBy("lock")
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
			_queueStateFlow.update {
				(playbackConnection.getSession(observer.sessionID)?.controller?.queue
					.takeIf { it != PlaybackQueue.UNSET } ?: localUNSET).also { new ->
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
		_queueStateFlow.value = localUNSET
		queueCollectorJob = scope.launch {
			val owner = Any()
			var listenerJob: Job? = null
			playbackConnection.observeCurrentSession().distinctUntilChanged()
				.transform { session ->
					listenerJob?.cancel()
					if (session == null) {
						emit(PlaybackQueue.UNSET)
						return@transform
					}
					val channel = Channel<PlaybackQueue>(Channel.CONFLATED)
					listenerJob = launch {
						try {
							session.controller.acquireObserver(owner).let { observer ->
								val get = observer.getAndObserveQueueChange { event ->
									channel.trySend(event.new)
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
				.collect(_queueStateFlow)
		}
	}
}
