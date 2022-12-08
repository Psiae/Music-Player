package com.flammky.musicplayer.media.mediaconnection.playback.real

import android.os.Handler
import android.os.Looper
import com.flammky.musicplayer.media.R
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.guava.future
import kotlin.time.Duration

class RealPlaybackConnection(
	private val looper: Looper,
	delegator: Delegator? = null
) : PlaybackConnection {

	private val handler = Handler(looper)
	private val dispatcher = handler.asCoroutineDispatcher()
	private val scope = CoroutineScope(dispatcher + SupervisorJob())

	private val _controller = Controller(
		looper,
		scope,
		delegator
	)

	override suspend fun <R> withControllerContext(
		block: suspend PlaybackConnection.Controller.() -> R
	): R {
		return if (Looper.myLooper() == looper) {
			_controller.block()
		} else {
			withContext(dispatcher) { _controller.block() }
		}
	}

	override suspend fun <R> withControllerImmediateContext(
		block: suspend PlaybackConnection.Controller.() -> R
	): R {
		return if (Looper.myLooper() == looper) {
			_controller.block()
		} else {
			withContext(dispatcher.immediate) { _controller.block() }
		}
	}

	override fun postController(
		block: suspend PlaybackConnection.Controller.() -> Unit
	) {
		scope.launch { _controller.block() }
	}

	override fun immediatePostController(
		block: suspend PlaybackConnection.Controller.() -> Unit
	) {
		scope.launch(dispatcher.immediate) { _controller.block() }
	}

	override fun <R> postControllerCallback(
		block: suspend PlaybackConnection.Controller.() -> R
	): ListenableFuture<R> {
		return scope.future { _controller.block() }
	}

	override fun <R> immediatePostControllerCallback(
		block: suspend PlaybackConnection.Controller.() -> R
	): ListenableFuture<R> {
		return scope.future(dispatcher.immediate) { _controller.block() }
	}

	interface Delegator {
		suspend fun <R> suspendController(block: suspend PlaybackController.() -> R): R
		fun postController(block: PlaybackController.() -> Unit)
		fun <R> futureController(block: PlaybackController.() -> R): ListenableFuture<R>
	}

	@OptIn(ExperimentalStdlibApi::class)
	private class Controller(
		private val looper: Looper,
		private val scope: CoroutineScope,
		delegator: Delegator?
	) : PlaybackConnection.Controller {

		val dispatcher: MainCoroutineDispatcher = scope.coroutineContext[CoroutineDispatcher]
			as? MainCoroutineDispatcher ?: error("Should be Looper Dispatcher")

		private val repeatModeListeners = mutableListOf<(RepeatMode) -> Unit>()
		private val shuffleModeListeners = mutableListOf<(ShuffleMode) -> Unit>()
		private val discontinuityListeners = mutableListOf<(PlaybackEvent.ProgressDiscontinuity) -> Unit>()
		private val isPlayingListeners = mutableListOf<(Boolean) -> Unit>()
		private val durationListeners = mutableListOf<(Duration) -> Unit>()
		private val playbackSpeedListeners = mutableListOf<(Float) -> Unit>()
		private val queueChangeListeners = mutableListOf<(PlaybackEvent.QueueChange) -> Unit>()

		var delegate: Delegator? = delegator
			get() {
				checkInLooper()
				return field
			}
			set(value) {
				checkInLooper()
				// remove listener
				// init listener
				field = value
			}

		override suspend fun getRepeatMode(): RepeatMode {
			return delegate?.suspendController { repeatMode } ?: RepeatMode.OFF
		}

		override suspend fun setRepeatMode(mode: RepeatMode): Boolean {
			return delegate?.suspendController { setRepeatMode(mode) } ?: false
		}

		override suspend fun observeRepeatMode(): Flow<RepeatMode> {
			return callbackFlow {

				val listener: (RepeatMode) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				send(getRepeatMode())

				repeatModeListeners.add(listener)
				awaitClose { repeatModeListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getShuffleMode(): ShuffleMode {
			return delegate?.suspendController { shuffleMode } ?: ShuffleMode.OFF
		}

		override suspend fun setShuffleMode(mode: ShuffleMode): Boolean {
			return delegate?.suspendController { setShuffleMode(mode) } ?: false
		}

		override suspend fun observeShuffleMode(): Flow<ShuffleMode> {
			return callbackFlow {

				val listener: (ShuffleMode) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				send(getShuffleMode())

				shuffleModeListeners.add(listener)
				awaitClose { shuffleModeListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getProgress(): Duration {
			return delegate?.suspendController { progress } ?: PlaybackConstants.PROGRESS_UNSET
		}

		override suspend fun seekProgress(progress: Duration): Boolean {
			return delegate?.suspendController { seekProgress(progress) } ?: false
		}

		override suspend fun seekIndex(index: Int, startProgress: Duration): Boolean {
			return delegate?.suspendController { seekIndex(index, startProgress) } ?: false
		}

		override suspend fun observeProgressDiscontinuity(): Flow<PlaybackEvent.ProgressDiscontinuity> {
			return callbackFlow {

				val listener: (PlaybackEvent.ProgressDiscontinuity) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				discontinuityListeners.add(listener)
				awaitClose { discontinuityListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getBufferedProgress(): Duration {
			return delegate?.suspendController { bufferedProgress } ?: PlaybackConstants.PROGRESS_UNSET
		}

		override suspend fun getIsPlaying(): Boolean {
			return delegate?.suspendController { playing } ?: false
		}

		override suspend fun observeIsPlaying(): Flow<Boolean> {
			return callbackFlow<Boolean> {

				val listener: (Boolean) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				send(getIsPlaying())

				isPlayingListeners.add(listener)
				awaitClose { isPlayingListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getDuration(): Duration {
			return delegate?.suspendController { duration } ?: PlaybackConstants.DURATION_UNSET
		}

		override suspend fun observeDuration(): Flow<Duration> {
			return callbackFlow<Duration> {

				val listener: (Duration) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				send(getDuration())

				durationListeners.add(listener)
				awaitClose { durationListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getPlaybackSpeed(): Float {
			return delegate?.suspendController { playbackSpeed } ?: 1f
		}

		override suspend fun observePlaybackSpeed(): Flow<Float> {
			return callbackFlow {

				val listener: (Float) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				send(getPlaybackSpeed())

				playbackSpeedListeners.add(listener)
				awaitClose { playbackSpeedListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		override suspend fun getQueue(): PlaybackQueue {
			return delegate?.suspendController { queue } ?: PlaybackQueue.UNSET
		}

		override suspend fun setQueue(queue: PlaybackQueue): Boolean {
			return delegate?.suspendController { setQueue(queue) } ?: false
		}

		override suspend fun observeQueue(): Flow<PlaybackQueue> {
			return callbackFlow {
				observeQueueChange().collect { send(it.new) }
				awaitClose()
			}.flowOn(dispatcher)
		}

		override suspend fun observeQueueChange(): Flow<PlaybackEvent.QueueChange> {
			return callbackFlow {
				val listener: (PlaybackEvent.QueueChange) -> Unit = {
					scope.launch(dispatcher.immediate) { send(it) }
				}

				queueChangeListeners.add(listener)
				awaitClose { queueChangeListeners.remove(listener) }
			}.flowOn(dispatcher)
		}

		private fun checkInLooper() {
			check(Looper.myLooper() == looper) {
				"Wrong Looper, expect: $looper actual: ${Looper.myLooper()}"
			}
		}
	}
}
