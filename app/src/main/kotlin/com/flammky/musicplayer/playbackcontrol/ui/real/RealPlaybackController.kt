package com.flammky.musicplayer.playbackcontrol.ui.real

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackSession
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.ui.playbackcontrol.RealPlaybackControlPresenter
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class RealPlaybackController(
	sessionID: String,
	private val scope: CoroutineScope,
	private val presenter: RealPlaybackControlPresenter,
	private val playbackConnection: PlaybackConnection
) : PlaybackController(sessionID) {

	private val _stateLock = Any()

	@GuardedBy("_stateLock")
	private var _disposed = false

	private val _observers = mutableListOf<RealPlaybackObserver>()
	private val _sessionObserversMap = mutableMapOf<String, MutableList<(PlaybackSession?) -> Unit>>()

	override val disposed: Boolean
		get() = sync(_stateLock) { _disposed }

	override fun dispose() {
		sync(_stateLock) {
			if (_disposed) {
				return checkDisposedState()
			}
			scope.cancel()
			_disposed = true
			disposeObservers()
			checkDisposedState()
		}
		presenter.notifyControllerDisposed(this)
	}

	override fun createPlaybackObserver(
		coroutineContext: CoroutineContext
	): PlaybackObserver {
		return RealPlaybackObserver(
			controller = this,
			parentScope = scope,
			connection = playbackConnection
		).also {
			sync(_stateLock) { if (_disposed) it.dispose() else _observers.sync { add(it) } }
		}
	}

	override fun requestSeekAsync(
		position: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext) {
			val success = playbackConnection.getSession(sessionID)?.controller?.withContext { seekProgress(position) }
				?: false
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach { jobs.add(it.updateProgress()) }
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(progress: Float): Deferred<RequestResult> {
		return scope.async {
			val success = playbackConnection.getSession()?.controller?.withContext {
				seekProgress((duration.inWholeMilliseconds * progress).toLong().milliseconds)
			} ?: false
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach { jobs.add(it.updateProgress()) }
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(
		index: Int,
		startPosition: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext) {
			val success = playbackConnection.getSession(sessionID)?.controller?.withContext {
				seekIndex(index, startPosition)
			} ?: false
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateQueue())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	fun notifyObserverDisposed(observer: RealPlaybackObserver) {
		_observers.sync {
			remove(observer)
		}
	}

	private fun checkDisposedState() {
		check(Thread.holdsLock(_stateLock))
		check(!scope.isActive && _observers.sync { isEmpty() }) {
			"Controller was not disposed properly"
		}
	}

	private fun disposeObservers() {
		debugDisposeObservers()
	}

	private fun debugDisposeObservers() {
		_observers.sync {
			val actual = this
			val copy = ArrayList(this)
			var count = copy.size
			for (observer in copy) {
				observer.dispose()
				check(actual.size == --count && actual.firstOrNull() != observer) {
					"Observer $observer did not notify Controller ${this@RealPlaybackController} on disposal"
				}
			}
			check(actual.isEmpty() && count == 0) {
				"disposeObservers failed"
			}
		}
	}
}
