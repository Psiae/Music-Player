package com.flammky.musicplayer.playbackcontrol.ui.real

import android.os.Looper
import androidx.annotation.MainThread
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
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

	private val privateProgressCollectors = mutableListOf<suspend (Duration) -> Unit>()

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
	private var progressCollector: Job? = null
		set(value) {
			require(field?.isActive != true) {
				"DEBUG: ProgressCollector ($field) was still `Active` when replaced with $value"
			}
			field = value
		}
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

	private val _progressMSF = MutableStateFlow(PlaybackConstants.PROGRESS_UNSET)
	private val _bufferedProgressMSF = MutableStateFlow(PlaybackConstants.PROGRESS_UNSET)
	private val _durationMSF = MutableStateFlow(PlaybackConstants.DURATION_UNSET)

	override val progressStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _progressMSF.asStateFlow()
		}
	override val bufferedProgressStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _bufferedProgressMSF.asStateFlow()
		}
	override val durationStateFlow: StateFlow<Duration>
		get() {
			// Lazy Impl here
			return _durationMSF.asStateFlow()
		}

	init {
		require(scopeDispatcher.limitedParallelism(1) == scopeDispatcher) {
			"Default Scope should have `1` parallelism"
		}
		scope.launch {
			// start collection lazily
			watchProgression()
			/* watchQueue */
			/* watchConfiguration */
			awaitCancellation()
		}.invokeOnCompletion {
			// research
			Timber.d("RealPlaybackObserver, invokeOnCompletion($it)")
		}
	}

	override fun collectProgress(
		collectorScope: CoroutineScope,
		startInterval: Duration?,
		includeEvent: Boolean,
		nextInterval: suspend (isEvent: Boolean, progress: Duration, duration: Duration, speed: Float) -> Duration?
	): StateFlow<Duration> {
		val state = MutableStateFlow(PlaybackConstants.PROGRESS_UNSET)

		collectorScope.launch {
			val flow = collectPlaybackProgress(
				startIn = startInterval ?: Duration.ZERO,
				includeEvent = includeEvent,
				delay = nextInterval
			)
			flow.collect(state)
		}

		return state
	}

	override suspend fun updatePosition() {
		updateProgress()
		val progress = playbackConnection.getProgress()
		privateProgressCollectors.forEach { it(progress) }
	}

	private suspend fun watchProgression(): Job {
		return scope.launch {
			combine(
				_progressMSF.subscriptionCount,
				_bufferedProgressMSF.subscriptionCount,
				_durationMSF.subscriptionCount
			) { a, b, c ->
				maxOf(a, b, c)
			}.first {
				it > 0
			}
			startSharedProgressCollector()
		}
	}

	private suspend fun startSharedProgressCollector() {
		checkInDefaultDispatcher()
		if (progressCollector?.isActive == true) {
			return
		}
		startSharedDurationCollector()
		progressCollector = dispatchSharedProgressCollector()
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

	private fun dispatchSharedProgressCollector(): Job {
		var collectorJob: Job? = null
		val isPlayingListener: IsPlayingListener = {
			if (it) {
				if (collectorJob?.isActive != true) {
					collectorJob = collectSharedPlaybackProgress()
				}
			} else {
				collectorJob?.cancel()
				updateProgress()
			}
		}
		val discontinuityListener: ProgressDiscontinuityListener = {
			updateProgress()
		}
		isPlayingListeners.add(isPlayingListener)
		discontinuityListeners.add(discontinuityListener)
		return scope.launch {
			collectorJob = collectSharedPlaybackProgress()
			startIsPlayingCollector()
			startDiscontinuityCollector()
		}
	}

	@MainThread
	private fun resetProgressCollector() {
		progressCollector?.cancel()
		_progressMSF.value = PlaybackConstants.PROGRESS_UNSET
		_bufferedProgressMSF.value = PlaybackConstants.PROGRESS_UNSET
	}

	@MainThread
	private fun dispatchSharedDurationCollector(): Job {
		return scope.launch {
			collectPlaybackDuration()
		}
	}

	@MainThread
	private fun resetDurationCollector() {
		durationCollector?.cancel()
		_durationMSF.value = PlaybackConstants.DURATION_UNSET
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

	private suspend fun collectSharedPlaybackProgress(): Job {
		return scope.launch {
			var nextDelay: Duration

			do {
				updateProgress()
				nextDelay = progressCollectionDelay()
				if (nextDelay.isNegative()) break
				delay(nextDelay)
			} while (kotlin.coroutines.coroutineContext.job.isActive)

			check(!kotlin.coroutines.coroutineContext.isActive ||
				nextDelay == PlaybackConstants.DURATION_UNSET
			) {
				"should be DURATION_UNSET, don't forget to change this"
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
							}

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

			delay(startIn)

			job = doCollect()

			val progressUpdateListener: suspend (Duration) -> Unit = {
				send(it)
			}

			if (includeEvent) {
				privateProgressCollectors.add(progressUpdateListener)
				internalCollectPlaybackDiscontinuity { discontinuity ->
					// TODO: Check whether `Discontinuity` callback is more accurate than `getPosition`
					if (job?.isActive == true) send(discontinuity.newProgress) else job = doCollect()
				}
				internalCollectIsPlayingChange { playing ->
					// TODO: MediaController `IsPlaying` is somewhat not accurate, internally we had `playWhenReady` backing it
					if (playing) {
						if (job?.isActive == false) job = doCollect()
					} else {
						if (job?.isActive == true) job!!.cancel()
					}
				}
			}

			awaitClose {
				if (includeEvent) {
					privateProgressCollectors.remove(progressUpdateListener)
				}
			}
		}.flowOn(scopeDispatcher)
	}

	private suspend fun collectPlaybackDuration() {
		playbackConnection.observeDuration().distinctUntilChanged().collect {
			updateDuration()
			updateProgress()
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
			startDiscontinuityCollector()
		}
	}

	private suspend fun collectIsPlaying() {
		playbackConnection.observeIsPlaying().collect {
			if (!it) {
				progressCollector?.cancel()
			} else if (progressCollector?.isActive != true) {
				dispatchSharedProgressCollector()
			}
		}
	}

	private suspend fun updateDuration() {
		playbackConnection.joinContext {
			_durationMSF.update {
				getDuration().also { Timber.d("RealPlaybackObserver updateDuration $it") }
			}
		}
	}

	private suspend fun updateProgress() {
		playbackConnection.joinContext {
			_progressMSF.update {
				getProgress().also { Timber.d("RealPlaybackObserver updateProgress $it") }
			}
			_bufferedProgressMSF.update {
				getBufferedProgress()
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
