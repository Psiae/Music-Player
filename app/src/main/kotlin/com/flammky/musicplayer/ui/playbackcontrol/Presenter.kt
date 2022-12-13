package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackController
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal interface PlaybackControlPresenter {

	/**
	 * create a Playback Controller.
	 *
	 * @param owner the observer owner
	 * @param key the observer key
	 */
	fun createController(
		owner: Any,
		controllerContext: CoroutineContext,
		// ignore
		key: String? = null
	): PlaybackController

	/**
	 * Dispose the presenter, calling the function will also dispose all disposable created within
	 */
	fun dispose()

	fun dispose(owner: Any)
}

internal class RealPlaybackControlPresenter(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	private val lock = Any()

	init {
	}

	private var _disposed = false


	private val _controllersMap = mutableMapOf<Any, MutableList<RealPlaybackController>>()

	override fun dispose() {
		sync(lock) {
			if (_disposed) {
				return checkDisposedState()
			}
			_controllersMap.sync {
				val actual = this
				val owners = _controllersMap.keys
				for (owner in owners) disposeControllers(owner)
				check(actual.isEmpty()) {
					"Dispose Controllers failed"
				}
			}
			checkDisposedState()
		}
	}

	override fun dispose(owner: Any) {
		sync(lock) {
			disposeControllers(owner)
		}
	}

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun createController(
		owner: Any,
		controllerContext: CoroutineContext,
		key: String?
	): PlaybackController {
		// realistically speaking will there be such case ?
		val scopeJob = controllerContext[Job] ?: SupervisorJob()
		val scopeDispatcher = controllerContext[CoroutineDispatcher]?.let { dispatcher ->
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
			owner = owner,
			scope = CoroutineScope(context = supervisor + scopeDispatcher),
			presenter = this,
			playbackConnection
		).also { controller ->
			sync {
				if (_disposed) {
					return@sync controller.dispose()
				}
				_controllersMap.getOrPut(owner) { mutableListOf() }.add(controller)
			}
		}
	}

	fun notifyControllerDisposed(
		controller: RealPlaybackController
	) {
		_controllersMap.sync {
			get(controller.owner)?.remove(controller)
		}
	}

	private fun disposeControllers(
		owner: Any
	) {
		_controllersMap.sync {
			get(owner)?.let { controllers ->
				val copy = ArrayList(controllers)
				var size = copy.size
				for (controller in copy) {
					controller.dispose()
					check(controllers.size == --size)
				}
				check(controllers.isEmpty() && size == 0)
				remove(owner)
			}
		}
	}

	private fun checkDisposedState() {
		check(Thread.holdsLock(lock))
		check(_controllersMap.isEmpty()) {
			"controllersMap is not empty"
		}
	}
}
