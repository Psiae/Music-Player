package com.flammky.musicplayer.playbackcontrol.ui.real

import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackProgressionCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

typealias ProgressDiscontinuityListener = suspend (PlaybackConnection.ProgressDiscontinuity) -> Unit
typealias IsPlayingListener = suspend (Boolean) -> Unit

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
/* internal */ class /* Debug */ RealPlaybackObserver(
	private val scope: CoroutineScope,
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver {

	private val publicProgressCollectors = mutableListOf<MutableStateFlow<Duration>>()
	private val updateProgressRequestListener = mutableListOf<Pair<CoroutineScope, suspend () -> Unit>>()

	private val scopeContext: CoroutineContext = scope.coroutineContext

	private val scopeDispatcher: /* MainCoroutineDispatcher */ CoroutineDispatcher =
		requireNotNull(scopeContext[CoroutineDispatcher]) {
			"CoroutineScope should have `CoroutineDispatcher` provided"
		}

	private val scopeJob: Job =
		requireNotNull(scopeContext[Job]) {
			"CoroutineScope should have `Job` provided"
		}

	//
	// should they be a Separate class ?
	//
	private var durationCollector: Job? = null
		set(value) {
			require(field?.isActive != true) {
				"DEBUG: DurationCollector ($field) was still `Active` when replaced with $value"
			}
			field = value
		}
	private var discontinuityCollector: Job? = null
		set(value) {
			require(field?.isActive != true) {
				"DEBUG: DiscontinuityCollector ($field) was still `Active` when replaced with $value"
			}
			field = value
		}
	private var isPlayingCollector: Job? = null
		set(value) {
			require(field?.isActive != true) {
				"DEBUG: IsPlayingCollector ($field) was still `Active` when replaced with $value"
			}
			field = value
		}
	private var playWhenReadyCollector: Job? = null
		set(value) {
			require(field?.isActive != true) {
				"DEBUG: PlayWhenReadyCollector ($field) was still `Active` when replaced with $value"
			}
			field = value
		}

	private val isPlayingListeners = mutableListOf<IsPlayingListener>()
	private val discontinuityListeners = mutableListOf<ProgressDiscontinuityListener>()
	private val _durationMSF = MutableStateFlow(PlaybackConstants.DURATION_UNSET)

	init {
		require(scopeDispatcher.limitedParallelism(1) == scopeDispatcher) {
			"Default Scope should have `1` parallelism"
		}
	}

	override fun collectDuration(): StateFlow<Duration> {
		scope.launch {
			startSharedDurationCollector()
		}
		return _durationMSF.asStateFlow()
	}

	override fun createProgressionCollector(
		collectorScope: CoroutineScope,
		includeEvent: Boolean,
	): PlaybackProgressionCollector {
		val job = collectorScope.coroutineContext.job
		val dispatcher = collectorScope.coroutineContext[CoroutineDispatcher]?.limitedParallelism(1)
			?: scopeDispatcher
		val confinedScope = CoroutineScope(context = SupervisorJob(job) + dispatcher)
		return RealPlaybackProgressionCollector(this, confinedScope, playbackConnection, includeEvent)
	}

	override suspend fun updateProgress() {
		withContext(scopeContext) {
			updateProgressRequestListener.filter { it.first.isActive }
			updateProgressRequestListener.forEach { it.first.launch { it.second() }.join() }
		}
	}

	override fun dispose() {
		scope.cancel()
	}

	fun observeUpdateRequest(
		scope: CoroutineScope,
		onUpdateRequest: suspend () -> Unit
	) {
		scope.launch {
			updateProgressRequestListener.add(scope to onUpdateRequest)
		}
	}

	private suspend fun startSharedDurationCollector() {
		checkInDefaultDispatcher()
		if (durationCollector?.isActive == true) {
			return
		}
		durationCollector = dispatchSharedDurationCollector()
	}

	private suspend fun startDiscontinuityCollector() {
		checkInDefaultDispatcher()
		if (discontinuityCollector?.isActive == true) {
			return
		}
		discontinuityCollector = dispatchDiscontinuityCollector()
	}

	private suspend fun startIsPlayingCollector() {
		checkInDefaultDispatcher()
		if (isPlayingCollector?.isActive == true) {
			return
		}
		isPlayingCollector = dispatchIsPlayingCollector()
	}

	@MainThread
	private fun dispatchSharedDurationCollector(): Job {
		return scope.launch {
			collectPlaybackDuration()
		}
	}

	private fun dispatchDiscontinuityCollector(): Job {
		return scope.launch {

			playbackConnection.observeProgressDiscontinuity().collect { discontinuity ->
				discontinuityListeners.forEach { listener -> listener(discontinuity) }
			}
		}
	}

	@MainThread
	private fun dispatchIsPlayingCollector(): Job {
		return scope.launch {

			// TODO: IsPlayingChangedReason
			playbackConnection.observeIsPlaying().distinctUntilChanged().collect { playing ->
				isPlayingListeners.forEach { listener -> listener(playing) }
			}
		}
	}

	/**
	 * Collect the Playback Progress
	 *
	 * @param includeEvent whether to also emit on certain events that affect the `Progress`
	 * @param delay they delay for next collection, must be positive
	 * ** There's no guarantee on the requested delay **
	 */
	private suspend fun collectPlaybackProgress(
		startIn: Duration,
		includeEvent: Boolean,
		delay: suspend (
			isEvent: Boolean,
			current: Duration,
			duration: Duration,
			speed: Float,
		) -> Duration?
	): Flow<Duration> {
		return callbackFlow<Duration> {
			var nextDelay: Duration
			var job: Job?

			suspend fun doCollect(): Job {
				return scope.launch {
					do {
						var progress: Duration = PlaybackConstants.PROGRESS_UNSET
						var duration: Duration = PlaybackConstants.DURATION_UNSET
						var speed: Float = 1f

						playbackConnection.joinContext {
							progress = getProgress()
							duration = getDuration()
							speed = getPlaybackSpeed()
						}

						send(progress)
						nextDelay = delay(false, progress, duration, speed)
							?: run {
								val fromNextSecond = (1010 - progress.inWholeMilliseconds % 1000)
								(fromNextSecond * speed).toLong().milliseconds
							}.coerceIn(MIN_PROGRESS_COLLECTION_INTERVAL, MAX_PROGRESS_COLLECTION_INTERVAL)

						Timber.d("DEBUG: nextDelay: $nextDelay")

						if (nextDelay.isNegative()) {
							if (nextDelay == PlaybackConstants.DURATION_UNSET) {
								break
							} else {
								error(
									"""
									delay ($nextDelay) must not be Negative, use `null` for DEFAULT interval
									or `PlaybackConstants.DURATION_UNSET` to wait for next `event` if `includeEvent`
									is true otherwise the will be no more emission
									"""
								)
							}
						}
						delay(nextDelay)
					} while (coroutineContext.isActive)
				}
			}

			if (startIn.isPositive()) {
				delay(startIn)
			}

			send(playbackConnection.getProgress())

			if (includeEvent) {
				job = if (playbackConnection.getIsPlaying()) {
					doCollect()
				} else {
					null
				}
				internalCollectPlaybackDiscontinuity { discontinuity ->
					// TODO: Check whether `Discontinuity` callback is more accurate than `getPosition`
					if (job?.isActive != true && playbackConnection.getIsPlaying()) {
						job = doCollect()
					} else {
						send(playbackConnection.getProgress())
					}
				}
				internalCollectIsPlayingChange { playing ->
					// TODO: MediaController `IsPlaying` is somewhat not accurate, internally we had `playWhenReady` backing it
					if (playing) {
						if (job?.isActive != true) {
							job = doCollect()
						}
					} else {
						job?.cancel()
					}
				}
			} else {
				job = doCollect()
			}

			awaitClose()
		}.flowOn(scopeDispatcher)
	}

	private suspend fun collectPlaybackDuration() {
		playbackConnection.observeDuration().distinctUntilChanged().collect {
			updateDuration()
		}
	}

	private suspend fun internalCollectPlaybackDiscontinuity(
		onDiscontinuity: ProgressDiscontinuityListener
	) {
		checkInDefaultDispatcher()
		discontinuityListeners.add(onDiscontinuity)

		if (discontinuityCollector?.isActive != true) {
			startDiscontinuityCollector()
		}
	}

	private suspend fun internalCollectIsPlayingChange(
		onIsPlayingChanged: IsPlayingListener
	) {
		checkInDefaultDispatcher()
		isPlayingListeners.add(onIsPlayingChanged)

		if (isPlayingCollector?.isActive != true) {
			startIsPlayingCollector()
		}
	}

	private suspend fun updateDuration() {
		playbackConnection.joinContext {
			_durationMSF.update {
				getDuration().also { Timber.d("RealPlaybackObserver updateDuration $it") }
			}
		}
	}

	/**
	 * Calculate the delay
	 */
	@MainThread
	private suspend fun progressCollectionDelay(): Duration {
		return playbackConnection.joinContext {
			val speed = getPlaybackSpeed()

			require(speed > 0f) {
				"Playback Speed ($speed) should be > 0f"
			}

			val duration = getDuration()
				.takeIf { it != PlaybackConstants.DURATION_UNSET }
				?: Duration.ZERO

			require(duration >= Duration.ZERO) {
				"""
					Playback Duration should be constrained >= ${Duration.ZERO}, don't forget to change this
				"""
			}

			val progress = getProgress()
				.takeIf { it != PlaybackConstants.PROGRESS_UNSET }
				?: Duration.ZERO

			require(progress >= Duration.ZERO) {
				"""
					Playback Progress should be constrained > ${Duration.ZERO}, don't forget to change this
				"""
			}

			val left = duration - progress
			val nextSecond = (1010 - progress.inWholeMilliseconds % 1000).milliseconds

			Timber.d("DEBUG: ProgressCollectionDelay, progressMs: ${progress.inWholeMilliseconds} left: ${left.inWholeMilliseconds}, next: ${nextSecond.inWholeMilliseconds} ")

			val speedConstrained = minOf(left, nextSecond).inWholeMilliseconds / speed
			speedConstrained.toLong().let {
				if (it < 10) {
					PlaybackConstants.DURATION_UNSET
				} else {
					it.milliseconds.coerceAtMost(MAX_PROGRESS_COLLECTION_INTERVAL)
				}
			}
		}.also {
			Timber.d("RealPlaybackObserver progressCollectionInterval: $it")
		}
	}


	private fun release() {
		// impl
	}

	private fun checkInMainThread(lazyMsg: () -> Any) {
		check(Looper.myLooper() == Looper.getMainLooper(), lazyMsg)
	}

	companion object {
		private val MAX_PROGRESS_COLLECTION_INTERVAL: Duration = 1.seconds
		private val MIN_PROGRESS_COLLECTION_INTERVAL: Duration = 100.milliseconds
	}

	private suspend inline fun CoroutineScope.collectDiscontinuity(
		crossinline onDiscontinuity: suspend (PlaybackConnection.ProgressDiscontinuity) -> Unit
	) {
		withContext(scopeDispatcher) {

		}
	}


	private suspend fun checkInDefaultDispatcher() {
		check(coroutineContext[CoroutineDispatcher] == scopeDispatcher) {
			"DEBUG: not in Default Scope Dispatcher"
		}
	}
}
