package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackObserver
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
	 * create a Playback Observer.
	 *
	 * @param owner the observer owner
	 * @param scope the observer parent coroutine scope
	 * ** scope must have `CoroutineDispatcher` with confined parallelism provided, to be removed **
	 * ** cancelling the provided @param scope will also cancel the PlaybackObserver scope **
	 * @param key the key, there will be no duplicate observer within the same owner and key
	 */
	fun observePlayback(
		owner: Any,
		scope: CoroutineScope,
		// ignore
		key: String? = null
	): PlaybackObserver
}


internal class RealPlaybackControlPresenter(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	init {
	}

	private val observersMap = mutableMapOf<Any, MutableList<RealPlaybackObserver>>()
	private val controllersMap = mutableMapOf<Any, MutableList<RealPlaybackController>>()

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

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun observePlayback(
		owner: Any,
		scope: CoroutineScope,
		key: String?
	): PlaybackObserver {
		val scopeJob = requireNotNull(scope.coroutineContext[Job]) {
			"CoroutineScope $scope does Not have `Job` provided"
		}
		val scopeDispatcher = scope.coroutineContext[CoroutineDispatcher]
			?.let { dispatcher ->
				try {
					dispatcher.limitedParallelism(1)
				} catch (e: Exception) {
					if (e is IllegalStateException || e is UnsupportedOperationException) {
						null
					} else {
						throw e
					}
				}
			}
			?: NonBlockingDispatcherPool.get(1)
		check(scopeDispatcher.limitedParallelism(1) === scopeDispatcher) {
			"Dispatcher parallelism could not be confined to `1`"
		}
		val supervisor = SupervisorJob(scopeJob)
		return RealPlaybackObserver(
			owner = owner,
			scope = CoroutineScope(context = supervisor + scopeDispatcher),
			dispatchers = dispatchers,
			playbackConnection = playbackConnection
		).also { observer ->
			sync {
				observersMap.getOrPut(owner) { mutableListOf() }.add(observer)
				supervisor.invokeOnCompletion {
					sync { observersMap[owner]?.remove(observer) }
				}
			}
		}
	}

	fun notifySeekRequest(): List<Job> {
		return sync {
			val jobs = mutableListOf<Job>()
			observersMap.values.forEach { observers ->
				observers.forEach { jobs.add(it.notifySeekEvent()) }
			}
			jobs
		}
	}
}
