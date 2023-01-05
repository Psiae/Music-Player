package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.PlaybackQueue
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.r.RealPlaybackController
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

internal interface PlaybackControlPresenter {

	/**
	 * Initialize the presenter
	 * ** This function must be called before any other **
	 */
	fun initialize(
		coroutineContext: CoroutineContext,
		// State Saver
		viewModel: ViewModel
	)

	fun currentSessionID(): String?

	/**
	 * Observe the Id of the currently active Session, null will be emitted if there's none
	 *
	 * will not emit anything until [initialize] is called
	 * calling this function after [dispose] is called will return an empty flow
	 */
	fun observeCurrentSessionId(): Flow<String?>

	/**
	 * create a Playback Controller.
	 *
	 * @param sessionID the Session ID this controller should send command / observe onto
	 * @param coroutineContext optional CoroutineContext this controller will use to dispatch / observe
	 */
	fun createController(
		sessionID: String,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): PlaybackController

	/**
	 * Dispose the presenter, calling the function will also dispose all disposable created within
	 * this presenter
	 *
	 * after this method is called any further request will be ignored or will return an invalid value.
	 */
	fun dispose()

	interface ViewModel {
		// TODO: Saver DATA
	}
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class RealPlaybackControlPresenter(
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	private val _stateLock = Any()

	private var _disposed = false
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}
		set(value) {
			check(Thread.holdsLock(_stateLock))
			field = value
		}

	// Consider wrapping these into initialized instance

	private var _coroutineScope: CoroutineScope? = null
		get() {
			check(Thread.holdsLock(_stateLock))
			return requireNotNull(
				value = field,
				lazyMessage = {
					"CoroutineScope must already be initialized on get attempt, check for `initialized` " +
						"boolean instead"
				}
			)
		}
		set(value) {
			check(Thread.holdsLock(_stateLock))
			check(field == null) {
				"Coroutine Scope cannot be re-set"
			}
			field = value
		}

	private var _initialized = false
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}
		set(value) {
			check(Thread.holdsLock(_stateLock))
			check(value) {
				"cannot be set to false"
			}
			field = true
		}

	private var _playbackConnectionObserverJob: Job? = null
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}
		set(value) {
			check(Thread.holdsLock(_stateLock))
			check(field?.isActive != true) {
				"Job was set to $value when the old ($field) was still active"
			}
			field = value
		}

	private val _controllersMap = mutableMapOf<String, MutableList<RealPlaybackController>>()
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}

	private val _connectionSessionObservers = mutableMapOf<String, () -> Unit>()
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}

	override fun initialize(
		coroutineContext: CoroutineContext,
		viewModel: PlaybackControlPresenter.ViewModel
	) {
		sync(_stateLock) {
			if (_disposed || _initialized) return
			val dispatcher = coroutineContext[CoroutineDispatcher]?.let { dispatcher ->
				try {
					dispatcher.limitedParallelism(1)
				} catch (e: Exception) {
					when {
						e is IllegalStateException && e.message?.startsWith("Module") == true -> null
						e is UnsupportedOperationException -> null
						else -> error("Uncaught Exception $e")
					}
				}
			} ?: NonBlockingDispatcherPool.get(1)
			_coroutineScope = CoroutineScope(dispatcher + SupervisorJob(coroutineContext[Job]))
			_initialized = true
		}
	}

	override fun currentSessionID(): String? {
		sync(_stateLock) {
			if (_disposed || !_initialized) return null
		}
		return playbackConnection.getSession()?.id
	}

	// should we return listenable for the actual disposal ?
	override fun dispose() {
		sync(_stateLock) {
			if (_disposed) {
				return checkDisposedState()
			}
			_controllersMap.flatMap(Map.Entry<String, MutableList<RealPlaybackController>>::value)
				.also {
					_controllersMap.clear()
					_disposed = true
				}
		}.forEach(PlaybackController::dispose)
	}

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun createController(
		sessionID: String,
		coroutineContext: CoroutineContext
	): PlaybackController {
		val scope = sync(_stateLock) {
			if (_disposed) {
				return EmptyPlaybackController(sessionID)
			}
			check(_initialized) {
				"Presenter must be initialized"
			}
			_coroutineScope!!
		}
		val scopeJob = coroutineContext[Job]
			?: scope.coroutineContext.job
		val scopeDispatcher = coroutineContext[CoroutineDispatcher]?.let { dispatcher ->
			try {
				dispatcher.limitedParallelism(1)
			} catch (e: Exception) {
				when {
					e is IllegalStateException && e.message?.startsWith("Module") == true -> null
					e is UnsupportedOperationException -> null
					else -> error("Uncaught Exception $e")
				}
			}
		} ?: scope.coroutineContext[CoroutineDispatcher]!!
		check(scopeDispatcher.limitedParallelism(1) === scopeDispatcher) {
			"Dispatcher parallelism could not be confined to `1`"
		}
		val supervisor = SupervisorJob(scopeJob)
		return RealPlaybackController(
			sessionID = sessionID,
			scope = CoroutineScope(context = supervisor + scopeDispatcher),
			presenter = this,
			playbackConnection
		).also { controller ->
			sync(_stateLock) {
				if (_disposed) {
					return@sync controller.dispose()
				}
				_controllersMap.getOrPut(sessionID) { mutableListOf() }.add(controller)
			}
		}
	}

	override fun observeCurrentSessionId(): Flow<String?> {
		val scope = sync(_stateLock) {
			if (_disposed) return flow { }
			check(_initialized)
			_coroutineScope!!
		}
		return flow {
			val channel = Channel<String?>()
			val job = scope.launch {
				playbackConnection.observeCurrentSession()
					.map { it?.id }
					.distinctUntilChanged()
					.collect(channel::send)
			}
			runCatching {
				emitAll(channel.consumeAsFlow())
			}.onFailure { t ->
				if (t !is CancellationException) throw t
			}
			job.cancel()
		}
	}

	fun notifyControllerDisposed(
		controller: RealPlaybackController
	) {
		sync(_stateLock) {
			_controllersMap[controller.sessionID]?.remove(controller)
		}
	}

	private fun checkDisposedState() {
		check(Thread.holdsLock(_stateLock))
		check(_controllersMap.isEmpty()) {
			"controllersMap is not empty"
		}
	}

	private class EmptyPlaybackController(sessionID: String) : PlaybackController(sessionID) {



		override val disposed: Boolean = true

		override fun createPlaybackObserver(coroutineContext: CoroutineContext): PlaybackObserver {
			return Observer
		}

		override fun requestSeekAsync(
			position: Duration,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			return CompletableDeferred<RequestResult>().apply { cancel() }
		}

		override fun requestSeekAsync(index: Int, startPosition: Duration, coroutineContext: CoroutineContext): Deferred<RequestResult> {
			return CompletableDeferred<RequestResult>().apply { cancel() }
		}

		override fun requestSetPlayWhenReadyAsync(
			playWhenReady: Boolean,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSetRepeatModeAsync(
			repeatMode: RepeatMode,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSeekNextAsync(
			startPosition: Duration,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSeekPreviousAsync(
			startPosition: Duration,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSeekPreviousItemAsync(
			startPosition: Duration,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestPlayAsync(coroutineContext: CoroutineContext): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSetShuffleModeAsync(
			shuffleMode: ShuffleMode,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestCompareAndSetAsync(compareAndSet: CompareAndSetScope.() -> Unit): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun dispose() = Unit

		private object Observer : PlaybackObserver {

			override val disposed: Boolean = true

			override fun createDurationCollector(collectorContext: CoroutineContext): PlaybackObserver.DurationCollector {
				return DurationCollector
			}

			override fun createProgressionCollector(
				collectorContext: CoroutineContext,
				includeEvent: Boolean
			): PlaybackObserver.ProgressionCollector {
				return ProgressionCollector
			}

			override fun createQueueCollector(collectorContext: CoroutineContext): PlaybackObserver.QueueCollector {
				return QueueCollector
			}

			override fun createPropertiesCollector(collectorContext: CoroutineContext): PlaybackObserver.PropertiesCollector {
				TODO("Not yet implemented")
			}

			override fun dispose() = Unit

			object DurationCollector : PlaybackObserver.DurationCollector {
				override val disposed: Boolean = true
				override val durationStateFlow: StateFlow<Duration> = MutableStateFlow(PlaybackConstants.DURATION_UNSET)
				override fun startCollect(): Job = Job().apply { cancel() }
				override fun stopCollect(): Job = Job().apply { cancel() }
				override fun dispose() = Unit
			}

			object QueueCollector : PlaybackObserver.QueueCollector {
				override val disposed: Boolean = true
				override val queueStateFlow: StateFlow<PlaybackQueue> = MutableStateFlow(PlaybackConstants.QUEUE_UNSET)
				override fun startCollect(): Job = Job().apply { cancel() }
				override fun stopCollect(): Job = Job().apply { cancel() }
				override fun dispose() = Unit
			}

			object ProgressionCollector : PlaybackObserver.ProgressionCollector {
				override val disposed: Boolean = true
				override val positionStateFlow: StateFlow<Duration> = MutableStateFlow(PlaybackConstants.POSITION_UNSET)
				override val bufferedPositionStateFlow: StateFlow<Duration> = MutableStateFlow(
					PlaybackConstants.POSITION_UNSET)

				override fun startCollectPosition(): Job = Job().apply { cancel() }
				override fun stopCollectProgress(): Job = Job().apply { cancel() }
				override fun setIntervalHandler(
					handler: suspend (
						isEvent: Boolean,
						progress: Duration,
						duration: Duration,
						speed: Float
					) -> Duration?
				) = Unit
				override fun setCollectEvent(collectEvent: Boolean): Job = Job().apply { cancel() }
				override fun dispose() = Unit
			}
		}
	}
}
