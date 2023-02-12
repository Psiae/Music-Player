package com.flammky.musicplayer.player.presentation.r

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackEvent
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.playbackcontrol.presentation.r.RealPlaybackDurationCollector
import com.flammky.musicplayer.playbackcontrol.presentation.r.RealPlaybackProgressionCollector
import com.flammky.musicplayer.playbackcontrol.presentation.r.RealPlaybackPropertiesCollector
import com.flammky.musicplayer.playbackcontrol.presentation.r.RealPlaybackQueueCollector
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

typealias ProgressDiscontinuityListener = suspend (PlaybackEvent.PositionDiscontinuity) -> Unit
typealias IsPlayingListener = suspend (Boolean) -> Unit

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal class /* Debug */ RealPlaybackObserver(
    private val user: User,
    private val controller: RealPlaybackController,
    private val parentScope: CoroutineScope,
    private val connection: PlaybackConnection,
) : PlaybackObserver {

	private val _stateLock = Any()

	@GuardedBy("_stateLock")
	private var _disposed = false

	private val updateProgressRequestListener = mutableListOf<Pair<CoroutineScope, suspend () -> Unit>>()
	private val parentScopeContext: CoroutineContext = parentScope.coroutineContext

	private val parentScopeDispatcher: /* MainCoroutineDispatcher */ CoroutineDispatcher =
		requireNotNull(parentScopeContext[CoroutineDispatcher]) {
			"Parent CoroutineScope should have `CoroutineDispatcher` provided"
		}

	private val parentScopeJob: Job =
		requireNotNull(parentScopeContext[Job]) {
			"Parent CoroutineScope should have `Job` provided"
		}

	private val _durationCollectors = mutableListOf<PlaybackObserver.DurationCollector>()
	private val _progressionCollectors = mutableListOf<PlaybackObserver.ProgressionCollector>()
	private val _queueCollectors = mutableListOf<RealPlaybackQueueCollector>()
	private val _playbackPropertiesCollectors = mutableListOf<RealPlaybackPropertiesCollector>()

	override val disposed: Boolean
		get() = sync(_stateLock) { _disposed }

	init {
		require(parentScopeDispatcher.limitedParallelism(1) == parentScopeDispatcher) {
			"Parent Scope should have `1` parallelism"
		}
	}

	fun notifyCollectorDisposed(
		collector: PlaybackObserver.Collector
	) {
		check(collector.disposed) {
			"Collector $collector was Not disposed"
		}
		when (collector) {
			is PlaybackObserver.DurationCollector -> _durationCollectors.sync(_stateLock) {
				remove(collector)
			}
			is PlaybackObserver.ProgressionCollector -> _progressionCollectors.sync(_stateLock) {
				remove(collector)
			}
			is PlaybackObserver.QueueCollector -> _queueCollectors.sync(_stateLock) {
				remove(collector)
			}
			is PlaybackObserver.PropertiesCollector -> _playbackPropertiesCollectors.sync(_stateLock) {
				remove(collector)
			}
		}
	}

	override fun createDurationCollector(
		collectorContext: CoroutineContext
	): PlaybackObserver.DurationCollector {
		val collectorJob = collectorContext[Job]
			?: parentScopeJob
		val collectorDispatcher = collectorContext[CoroutineDispatcher]
		val tryLimit = collectorDispatcher?.limitedParallelism(1)
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === tryLimit }
			?: parentScopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackDurationCollector(
			user = user,
			parentObserver = this,
			scope = confinedScope,
			connection = connection,
		).also {
			sync(_stateLock) {
				// seems to be better solution than having to throw an exception
				if (_disposed) it.dispose() else _durationCollectors.sync { add(it) }
			}
		}
	}

	override fun createProgressionCollector(
		collectorContext: CoroutineContext,
		includeEvent: Boolean,
	): PlaybackObserver.ProgressionCollector {
		val collectorJob = collectorContext[Job]
			?: parentScopeJob
		val collectorDispatcher = collectorContext[CoroutineDispatcher]
		val tryLimit = collectorDispatcher?.limitedParallelism(1)
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === tryLimit }
			?: parentScopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackProgressionCollector(
			user = user,
			observer = this,
			scope = confinedScope,
			playbackConnection = connection,
			collectEvent = includeEvent
		).also {
			sync(_stateLock) { if (_disposed) it.dispose() else _progressionCollectors.sync { add(it) } }
		}
	}

	override fun createQueueCollector(
		collectorContext: CoroutineContext
	): PlaybackObserver.QueueCollector {
		val collectorJob = collectorContext[Job]
			?: parentScopeJob
		val collectorDispatcher = collectorContext[CoroutineDispatcher]
		val tryLimit = collectorDispatcher?.limitedParallelism(1)
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === tryLimit }
			?: parentScopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackQueueCollector(
			user,
			observer = this,
			scope = confinedScope,
			playbackConnection = connection
		).also {
			sync(_stateLock) { if (_disposed) it.dispose() else _queueCollectors.sync { add(it) } }
		}
	}

	override fun createPropertiesCollector(
		collectorContext: CoroutineContext
	): PlaybackObserver.PropertiesCollector {
		val collectorJob = collectorContext[Job]
			?: parentScopeJob
		val collectorDispatcher = collectorContext[CoroutineDispatcher]
		val tryLimit = collectorDispatcher?.limitedParallelism(1)
		val dispatcher = collectorDispatcher?.limitedParallelism(1)
			.takeIf { it === tryLimit }
			?: parentScopeDispatcher
		val supervisor = SupervisorJob(collectorJob)
		val confinedScope = CoroutineScope(context = supervisor + dispatcher)
		return RealPlaybackPropertiesCollector(
			user = user,
			scope = confinedScope,
			parentObserver = this,
			connection = connection
		).also {
			sync(_stateLock) { if (_disposed) it.dispose() else _playbackPropertiesCollectors.sync { add(it) } }
		}
	}

	fun updateProgress(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			updateProgressRequestListener.removeAll { !it.first.isActive }
			updateProgressRequestListener.forEach {
				it.first.launch { it.second() }.also { job -> jobs.add(job) }
			}
			jobs.joinAll()
		}
	}

	fun updateQueue(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			val queueCollectors = sync(_stateLock) {
				_queueCollectors.sync { ArrayList(this) }
			}
			queueCollectors.forEach { jobs.add(it.updateQueue()) }
			updateProgress().join()
			jobs.joinAll()
		}
	}

	fun updatePlayWhenReady(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			val playWhenReadyCollectors = sync(_stateLock) {
				_playbackPropertiesCollectors.sync { ArrayList(this) }
			}
			playWhenReadyCollectors.forEach {
				jobs.add(it.updatePlayWhenReady())
			}
		}
	}

	fun updateRepeatMode(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			val repeatModeCollectors = sync(_stateLock) {
				_playbackPropertiesCollectors.sync { ArrayList(this) }
			}
			repeatModeCollectors.forEach {
				jobs.add(it.updateRepeatMode())
			}
		}
	}

	fun updateShuffleMode(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			val shuffleModeCollectors = sync(_stateLock) {
				_playbackPropertiesCollectors.sync { ArrayList(this) }
			}
			shuffleModeCollectors.forEach {
				jobs.add(it.updateShuffleMode())
			}
		}
	}

	override fun dispose() {
		sync(_stateLock) {
			if (_disposed) {
				return /* checkDisposedState() */
			}
			disposeCollectors()
			_disposed = true
		}
		controller.notifyObserverDisposed(this)
	}

	private fun disposeCollectors() {
		_durationCollectors.sync {
			val actual = this
			val copy = ArrayList(this)
			var count = copy.size
			for (collector in copy) {
				collector.dispose()
				check(actual.size == --count) {
					"PlaybackCollector($collector) did not notify " +
						" PlaybackObserver(${this@RealPlaybackObserver}) after `dispose` call"
				}
			}
			check(actual.isEmpty()) {
				"Dispose failed, durationCollector was not empty"
			}
		}
		_progressionCollectors.sync {
			val actual = this
			val copy = ArrayList(this)
			var count = copy.size
			for (collector in copy) {
				collector.dispose()
				check(actual.size == --count) {
					"PlaybackCollector($collector) did not notify " +
						"PlaybackObserver(${this@RealPlaybackObserver}) after `dispose` call"
				}
			}
			check(actual.isEmpty()) {
				"Dispose failed, progressionCollector was not empty"
			}
		}
		_queueCollectors.sync {
			val actual = this
			val copy = ArrayList(this)
			var count = copy.size
			for (collector in this) {
				collector.dispose()
				check(actual.size == --count) {
					"PlaybackCollector($collector) did not notify " +
						"PlaybackObserver(${this@RealPlaybackObserver}) after `dispose` call"
				}
			}
			check(actual.isEmpty()) {
				"Dispose failed, queueCollector was not empty"
			}
		}
	}

	fun observeUpdateRequest(
		scope: CoroutineScope,
		onUpdateRequest: suspend () -> Unit
	) {
		scope.launch {
			updateProgressRequestListener.add(scope to onUpdateRequest)
		}
	}
}
