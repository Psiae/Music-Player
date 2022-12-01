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

	private val observers = mutableListOf<RealPlaybackObserver>()

	private val _owners = mutableMapOf<Any, MutableMap<String?, Any>>()

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
		return RealPlaybackController(
			scope = CoroutineScope(context = SupervisorJob(scopeJob) + scopeDispatcher),
			presenter = this,
			playbackConnection
		)
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
		return RealPlaybackObserver(
			owner = owner,
			scope = CoroutineScope(context = SupervisorJob(scopeJob) + scopeDispatcher),
			dispatchers = dispatchers,
			playbackConnection = playbackConnection
		).also {
			sync {
				observers.add(it)
			}
		}
	}

	fun notifySeekRequest(): List<Job> {
		return sync {
			val jobs = mutableListOf<Job>()
			observers.forEach { jobs.add(it.notifySeekEvent()) }
			jobs
		}
	}
}
