package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackController
import kotlinx.coroutines.*

internal interface PlaybackControlPresenter {

	/**
	 * create a Playback Controller.
	 *
	 * @param owner the observer owner
	 * @param scope the observer parent coroutine scope
	 * ** cancelling this scope will also cancel the controller scope **
	 * @param key the observer key
	 */
	fun createController(
		owner: Any,
		scope: CoroutineScope,
		// ignore
		key: String? = null
	): PlaybackController

	/**
	 * Dispose the presenter, calling the function will also dispose all disposable created within
	 */
	fun dispose()
}

internal class RealPlaybackControlPresenter(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	init {
	}

	private val controllersMap = mutableMapOf<Any, MutableList<RealPlaybackController>>()

	override fun dispose() {
		sync {
			for (controllers in controllersMap.values) {
				controllers.forEach { it.dispose() }
			}
		}
	}

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun createController(
		owner: Any,
		scope: CoroutineScope,
		key: String?
	): PlaybackController {
		// realistically speaking will there be such case ?
		val scopeJob = requireNotNull(scope.coroutineContext[Job]) {
			"CoroutineScope $scope does Not have `Job` provided"
		}
		val scopeDispatcher = scope.coroutineContext[CoroutineDispatcher]?.let { dispatcher ->
			try {
				dispatcher.limitedParallelism(1)
			} catch (e: Exception) {
				if (e is IllegalStateException || e is UnsupportedOperationException) {
					null
				} else {
					throw e
				}
			}
		} ?: NonBlockingDispatcherPool.get(1)
		check(scopeDispatcher.limitedParallelism(1) === scopeDispatcher) {
			"Dispatcher parallelism could not be confined to `1`"
		}
		val supervisor = SupervisorJob(scopeJob)
		return RealPlaybackController(
			scope = CoroutineScope(context = supervisor + scopeDispatcher),
			presenter = this,
			playbackConnection
		).also { controller ->
			sync {
				controllersMap.getOrPut(owner) { mutableListOf() }.add(controller)
				supervisor.invokeOnCompletion {
					sync { controllersMap[owner]?.remove(controller) }
				}
			}
		}
	}
}
