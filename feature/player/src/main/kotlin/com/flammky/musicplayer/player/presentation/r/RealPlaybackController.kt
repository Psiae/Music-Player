@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.flammky.musicplayer.player.presentation.r

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

internal class RealPlaybackController(
	user: User,
	private val scope: CoroutineScope,
	private val playbackConnection: PlaybackConnection,
	private val disposeHandle: (RealPlaybackController) -> Unit
) : PlaybackController(user) {

	private val _stateLock = Any()

	@GuardedBy("_stateLock")
	private var _disposed = false

	private val _observers = mutableListOf<RealPlaybackObserver>()

	override val disposed: Boolean
		get() = _disposed

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
		disposeHandle.invoke(this)
	}

	override fun createPlaybackObserver(
		coroutineContext: CoroutineContext
	): PlaybackObserver {
		return RealPlaybackObserver(
			user = user,
			controller = this,
			parentScope = scope,
			connection = playbackConnection
		).also {
			sync(_stateLock) { if (_disposed) it.dispose() else _observers.sync { add(it) } }
		}
	}

	// We obviously don't have to request for the controller everytime
	// but that's for later

	override fun requestSeekPositionAsync(
		expectId: String,
		expectDuration: Duration,
		percent: Float
	): Deferred<RequestResult> {
		return scope.async {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekPosition(
						expectId,
						expectDuration,
						percent
					)
				}
			}.getOrElse {
				false
			}
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
		position: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekPosition(position)
				}
			}.getOrElse {
				false
			}
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
	): Deferred<PlaybackController.RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekIndex(index, startPosition)
				}
			}.getOrElse {
				false
			}
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

	override fun requestSeekAsync(
		expectFromIndex: Int,
		expectFromId: String,
		expectToIndex: Int,
		expectToId: String,
		startPosition: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		Timber.d("RealPlaybackController, requestSeekAsync($expectFromIndex, $expectFromId $expectToIndex, $expectToId)")
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					requestSeekIndexAsync(expectFromIndex, expectFromId, expectToIndex, expectToId)
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch {
						/*val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateQueue())
						}
						jobs.joinAll()*/
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekNextAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekNext()
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateProgress())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekPreviousAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekPrevious()
				}
			}.getOrElse {
				false
			}
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

	override fun requestSeekPreviousItemAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					seekPreviousMediaItem()
				}
			}.getOrElse {
				false
			}
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

	override fun requestCompareAndSetAsync(
		compareAndSet: CompareAndSetScope.() -> Unit)
	: Deferred<RequestResult> {
		TODO("Not yet implemented")
	}

	override fun requestPlayAsync(
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					play()
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updatePlayWhenReady())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSetPlayWhenReadyAsync(
		playWhenReady: Boolean,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					setPlayWhenReady(playWhenReady)
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updatePlayWhenReady())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSetRepeatModeAsync(
		repeatMode: RepeatMode,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					setRepeatMode(repeatMode)
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateRepeatMode())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}


	}

	override fun requestSetShuffleModeAsync(
		shuffleMode: ShuffleMode,
		coroutineContext: CoroutineContext
	): Deferred<RequestResult> {
		return scope.async(coroutineContext.minusKey(CoroutineDispatcher)) {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					setShuffleMode(shuffleMode)
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateShuffleMode())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestToggleRepeatModeAsync(): Deferred<RequestResult> {
		return scope.async {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					toggleRepeatMode()
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateRepeatMode())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestToggleShuffleModeAsync(): Deferred<RequestResult> {
		return scope.async {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					toggleShuffleMode()
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach {
							jobs.add(it.updateShuffleMode())
						}
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestMoveAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Deferred<RequestResult> {
		return scope.async {
			val success = runCatching {
				playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
					requestMoveAsync(from, expectFromId, to, expectToId)
				}
			}.getOrElse {
				false
			}
			RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
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
