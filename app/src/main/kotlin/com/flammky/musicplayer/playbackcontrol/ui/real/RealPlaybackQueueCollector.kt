package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackController
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RealPlaybackQueueCollector(
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
		scope.launch {
			startObserveInternal()
		}
		return scope.launch(start = CoroutineStart.LAZY) {
			queueStateFlow.first { it !== localUNSET }
		}
	}

	private suspend fun startObserveInternal() {
		if (queueCollectorJob?.isActive != true) {
			queueCollectorJob = scope.launch {

				callbackFlow {
					val owner = Any()
					var controller: PlaybackController? = null
					playbackConnection.observeCurrentSession().distinctUntilChanged().collect {
						controller?.releaseObserver(owner)
						controller = it?.controller
						controller?.withContext {
							acquireObserver(owner).getAndObserveQueueChange {
								scope.launch { send(it.new) }
							}
						}?.let { send(it) }
					}

					awaitClose { controller?.releaseObserver(owner) }
				}.collect(_queueStateFlow)
			}
		}
	}

	override fun stopObserve(): Job {
		TODO("Not yet implemented")
	}

	override fun dispose() {
		observer.notifyCollectorDisposed(this)
	}
}
