package com.flammky.musicplayer.player.presentation.presenter

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.GuardedBy
import androidx.core.net.toUri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackConstants
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.r.RealPlaybackController
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * Essentially the actual bundle of use-case
 */
internal interface PlaybackControlPresenter {

	/**
	 * Initialize the presenter
	 * ** This function must be called before any other **
	 */
	fun initialize(
		// State Saver
		viewModel: ViewModel
	)

	val auth: Auth

	val mediaRepo: MediaRepo

	/**
	 * create a Playback Controller.
	 *
	 * @param user the user this controller should send command / observe it's session onto
	 * @param coroutineContext optional CoroutineContext this controller will use to dispatch / observe
	 */
	fun createUserPlaybackController(
		user: User,
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
		val mainCoroutineContext: CoroutineContext
		// TODO: Saver DATA
	}

	interface Auth {
		val currentUser: User?
		fun observeCurrentUser(): Flow<User?>
	}

	interface MediaRepo {
		fun getCachedArtwork(id: String): Any?
		fun getCachedMetadata(id: String): MediaMetadata?
		fun observeArtwork(id: String): Flow<Any?>
		fun observeMetadata(id: String): Flow<MediaMetadata?>
	}
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class ExpectPlaybackControlPresenter(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection,
	private val authService: AuthService,
	// change to provider instead
	private val mediaMetadataCacheRepository: MediaMetadataCacheRepository,
	private val artworkProvider: ArtworkProvider,
	private val metadataProvider: MetadataProvider
): PlaybackControlPresenter {

	private val _stateLock = Any()

	/* @Volatile */
	@GuardedBy("_stateLock")
	private var _actual: Actual? = null

	override val auth: PlaybackControlPresenter.Auth
		get() = _actual?.auth
			?: uninitializedError {
				// msg
			}

	override val mediaRepo: PlaybackControlPresenter.MediaRepo
		get() = _actual?.mediaRepo
			?: uninitializedError {
				// msg
			}

	override fun initialize(viewModel: PlaybackControlPresenter.ViewModel) {
		sync(_stateLock) {
			if (_actual != null) {
				alreadyInitializedError()
			}
			_actual = Actual(viewModel)
			_initialized = true
			_coroutineScope = CoroutineScope(viewModel.mainCoroutineContext + SupervisorJob())
		}
	}

	override fun dispose() {
		sync(_stateLock) {
			_actual?.dispose()
		}
	}

	private fun uninitializedError(lazyMsg: () -> Any? = {}): Nothing =
		error("PlaybackControlPresenter was not Initialized, msg: ${lazyMsg().toString()}")
	private fun alreadyInitializedError(lazyMsg: () -> Any? = {}): Nothing =
		error("PlaybackControlPresenter was already Initialized, msg: ${lazyMsg().toString()}")

	private inner class Actual(
		private val viewModel: PlaybackControlPresenter.ViewModel
	): PlaybackControlPresenter {

		private var _disposed = false
			get() {
				check(Thread.holdsLock(_stateLock))
				return field
			}
			set(value) {
				check(Thread.holdsLock(_stateLock))
				field = value
			}

		private val supervisorScope = CoroutineScope(viewModel.mainCoroutineContext + SupervisorJob())

		init {
		}

		override fun initialize(
			viewModel: PlaybackControlPresenter.ViewModel
		) = error("Initialized via constructor")

		override val auth = object : PlaybackControlPresenter.Auth {

			override val currentUser: User?
				get() = authService.currentUser

			override fun observeCurrentUser(): Flow<User?> =
				flow {
					val channel = Channel<User?>() {
						error("Undelivered Element on suspend overflow, user=$it")
					}
					supervisorScope.launch(viewModel.mainCoroutineContext.minusKey(Job)) {
						authService.observeCurrentUser().collect { channel.send(it) }
					}
					channel.consumeAsFlow().collect(this)
				}
		}

		override val mediaRepo = object : PlaybackControlPresenter.MediaRepo {

			private val artJobs = mutableMapOf<String, Job>()
			private val metadataJobs = mutableMapOf<String, Job>()

			override fun getCachedArtwork(id: String): Any? = mediaMetadataCacheRepository.getArtwork(id + "_raw")
			override fun getCachedMetadata(id: String): MediaMetadata? = mediaMetadataCacheRepository.getMetadata(id)

			override fun observeArtwork(id: String): Flow<Any?> {
				val cached = getCachedArtwork(id) != null
				val contains = artJobs.contains(id)
				Timber.d("observeArtwork: id=$id, cached=$cached contains=$contains")
				if (!cached) {
					supervisorScope.launch {
						if (artJobs.contains(id)) {
							return@launch
						}
						artJobs[id] = launch {
							val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
								.setMemoryCacheAllowed(true)
								.setStoreMemoryCacheAllowed(true)
								.setUri(id.toUri())
								.build()
							artworkProvider.request(req).await()
						}
						runCatching {
							artJobs[id]!!.join()
						}
						artJobs.remove(id)
					}
				}
				return mediaMetadataCacheRepository.observeArtwork(id + "_raw")
			}

			override fun observeMetadata(id: String): Flow<MediaMetadata?> {
				metadataProvider.requestAsync(id)
				return mediaMetadataCacheRepository.observeMetadata(id)
			}
		}

		override fun createUserPlaybackController(
			user: User,
			coroutineContext: CoroutineContext
		): PlaybackController {
			TODO("Not yet implemented")
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
	}

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

	private val _controllersMap = mutableMapOf<String, MutableList<RealPlaybackController>>()
		get() {
			check(Thread.holdsLock(_stateLock))
			return field
		}

	//
	// TODO move
	//

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun createUserPlaybackController(
		user: User,
		coroutineContext: CoroutineContext
	): PlaybackController {
		val scope = sync(_stateLock) {
			if (_disposed) {
				return EmptyPlaybackController(user)
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
			user = user,
			scope = CoroutineScope(context = supervisor + scopeDispatcher),
			playbackConnection = playbackConnection,
			disposeHandle = {
				notifyControllerDisposed(it)
			}
		).also { controller ->
			sync(_stateLock) {
				if (_disposed) {
					return@sync controller.dispose()
				}
				_controllersMap.getOrPut(user.uid + "_" + user.verify) { mutableListOf() }.add(controller)
			}
		}
	}

	fun notifyControllerDisposed(
		controller: RealPlaybackController
	) {
		sync(_stateLock) {
			_controllersMap[controller.user.uid + "_" + controller.user.verify]?.remove(controller)
		}
	}

	private fun checkDisposedState() {
		check(Thread.holdsLock(_stateLock))
		check(_controllersMap.isEmpty()) {
			"controllersMap is not empty"
		}
	}

	private class EmptyPlaybackController(user: User) : PlaybackController(user) {

		override fun requestToggleRepeatModeAsync(): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestToggleShuffleModeAsync(): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSeekPositionAsync(
			expectId: String,
			expectDuration: Duration,
			percent: Float
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestSeekAsync(
			expectFromIndex: Int,
			expectFromId: String,
			expectToIndex: Int,
			expectToId: String,
			startPosition: Duration,
			coroutineContext: CoroutineContext
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

		override fun requestMoveAsync(
			from: Int,
			expectFromId: String,
			to: Int,
			expectToId: String
		): Deferred<RequestResult> {
			TODO("Not yet implemented")
		}

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
				override val queueStateFlow: StateFlow<OldPlaybackQueue> = MutableStateFlow(PlaybackConstants.QUEUE_UNSET)
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
					handler: (
						isEvent: Boolean,
						progress: Duration,
						bufferedProgress: Duration,
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
