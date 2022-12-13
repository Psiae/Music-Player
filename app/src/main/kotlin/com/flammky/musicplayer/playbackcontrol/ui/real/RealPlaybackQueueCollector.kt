package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class RealPlaybackQueueCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver.QueueCollector {

	private val localUNSET = PlaybackQueue.UNSET.copy()

	private val _queueStateFlow = MutableStateFlow(localUNSET)

	private var queueCollectorJob: Job? = null

	override val queueStateFlow: StateFlow<PlaybackQueue> = _queueStateFlow.asStateFlow()

	init {
		val scopeDispatcher = scope.coroutineContext[CoroutineDispatcher]
		require(scopeDispatcher?.limitedParallelism(1) === scopeDispatcher) {
			"CoroutineScope dispatcher was not confined"
		}
	}

	override fun startObserve(): Job {
		val launch = scope.launch {
			startObserveInternal()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			launch.join()
			_queueStateFlow.first { it !== localUNSET }
		}
	}

	override fun stopObserve(): Job {
		return scope.launch {
			queueCollectorJob?.cancel()
		}
	}

	override fun dispose() {
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
					try {
						listenerJob = launch {
							val channel = Channel<PlaybackQueue>()
							session.controller.acquireObserver(owner).let { observer ->
								val get = observer.getAndObserveQueueChange { event ->
									channel.trySend(event.new)
								}
								channel.send(get)
							}
							emitAll(channel.consumeAsFlow())
						}.apply { join() }
					} finally {
						session.controller.releaseObserver(owner)
					}
				}
				.onCompletion {
					listenerJob?.cancel()
				}
				.collect(_queueStateFlow)
		}
	}
}
