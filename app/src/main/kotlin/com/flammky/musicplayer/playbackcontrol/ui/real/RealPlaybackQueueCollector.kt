package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackQueue
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackQueueCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class RealPlaybackQueueCollector(
	private val observer: RealPlaybackObserver,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackQueueCollector {

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
				playbackConnection.withControllerContext { observeQueue() }.collect(_queueStateFlow)
			}
		}
	}
}
