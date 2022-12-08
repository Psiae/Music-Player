package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackConstants
import com.flammky.musicplayer.media.playback.PlaybackEvent
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackProgressionCollector
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackQueueCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

typealias ProgressDiscontinuityListener = suspend (PlaybackEvent.ProgressDiscontinuity) -> Unit
typealias IsPlayingListener = suspend (Boolean) -> Unit

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
/* internal */ class /* Debug */ RealPlaybackObserver(
	val owner: Any,
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
		val collectorJob = collectorScope.coroutineContext.job
		val collectorDispatcher = collectorScope.coroutineContext[CoroutineDispatcher]
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === collectorDispatcher }
			?: scopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackProgressionCollector(
			observer = this,
			scope = confinedScope,
			playbackConnection = playbackConnection,
			collectEvent = includeEvent
		)
	}

	override fun createQueueCollector(
		collectorScope: CoroutineScope
	): PlaybackQueueCollector {
		val collectorJob = collectorScope.coroutineContext.job
		val collectorDispatcher = collectorScope.coroutineContext[CoroutineDispatcher]
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === collectorDispatcher }
			?: scopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackQueueCollector(
			observer = this,
			scope = confinedScope,
			playbackConnection = playbackConnection
		)
	}

	override fun updateProgress(): Job {
		return scope.launch {
			val jobs = mutableListOf<Job>()
			updateProgressRequestListener.filter { it.first.isActive }
			updateProgressRequestListener.forEach {
				it.first.launch { it.second() }.also { job -> jobs.add(job) }
			}
			jobs.joinAll()
		}
	}

	override fun dispose() {
		scope.cancel()
	}

	fun notifySeekEvent(): Job = updateProgress()

	fun observeUpdateRequest(
		scope: CoroutineScope,
		onUpdateRequest: suspend () -> Unit
	) {
		scope.launch {
			updateProgressRequestListener.add(scope to onUpdateRequest)
		}
	}

	private suspend fun startSharedDurationCollector() {
		if (durationCollector?.isActive == true) {
			return
		}
		durationCollector = dispatchSharedDurationCollector()
	}

	private suspend fun startDiscontinuityCollector() {
		if (discontinuityCollector?.isActive == true) {
			return
		}
		discontinuityCollector = dispatchDiscontinuityCollector()
	}

	private suspend fun startIsPlayingCollector() {
		if (isPlayingCollector?.isActive == true) {
			return
		}
		isPlayingCollector = dispatchIsPlayingCollector()
	}

	private fun dispatchSharedDurationCollector(): Job {
		return scope.launch {
			collectPlaybackDuration(::updateDuration)
		}
	}

	private fun dispatchDiscontinuityCollector(): Job {
		return scope.launch {
			collectPlaybackDiscontinuity(::notifyProgressDiscontinuity)
		}
	}

	private fun dispatchIsPlayingCollector(): Job {
		return scope.launch {
			collectIsPlayingChange(::notifyIsPlayingChange)
		}
	}

	private suspend fun collectIsPlayingChange(
		collector: FlowCollector<Boolean>
	) {
		playbackConnection.withControllerContext(PlaybackConnection.Controller::observeIsPlaying)
			.distinctUntilChanged()
			.collect(collector)
	}

	private suspend fun collectPlaybackDiscontinuity(
		collector: FlowCollector<PlaybackEvent.ProgressDiscontinuity>
	) {
		playbackConnection.withControllerContext(PlaybackConnection.Controller::observeProgressDiscontinuity)
			.collect(collector)
	}

	private suspend fun collectPlaybackDuration(
		collector: FlowCollector<Duration>
	) {
		playbackConnection.withControllerContext(PlaybackConnection.Controller::observeDuration)
			.distinctUntilChanged()
			.collect(collector)
	}

	private suspend fun notifyIsPlayingChange(
		isPlaying: Boolean
	) {
		isPlayingListeners.forEach { listener -> listener(isPlaying) }
	}

	private suspend fun notifyProgressDiscontinuity(
		discontinuity: PlaybackEvent.ProgressDiscontinuity
	) {
		discontinuityListeners.forEach { listener -> listener(discontinuity) }
	}

	private suspend fun updateDuration(
		duration: Duration
	) {
		_durationMSF.update { duration }
	}

	companion object {
		private val MAX_PROGRESS_COLLECTION_INTERVAL: Duration = 1.seconds
		private val MIN_PROGRESS_COLLECTION_INTERVAL: Duration = 100.milliseconds
	}
}
