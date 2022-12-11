package com.flammky.musicplayer.playbackcontrol.ui.real

import androidx.annotation.GuardedBy
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackEvent
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

typealias ProgressDiscontinuityListener = suspend (PlaybackEvent.ProgressDiscontinuity) -> Unit
typealias IsPlayingListener = suspend (Boolean) -> Unit

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
/* internal */ class /* Debug */ RealPlaybackObserver(
	private val parentScope: CoroutineScope,
	private val playbackConnection: PlaybackConnection
) : PlaybackObserver {

	@GuardedBy("this")
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
	private val _queueCollectors = mutableListOf<PlaybackObserver.QueueCollector>()

	init {
		require(parentScopeDispatcher.limitedParallelism(1) == parentScopeDispatcher) {
			"Parent Scope should have `1` parallelism"
		}
	}

	fun notifyCollectorDisposed(
		collector: PlaybackObserver.Collector
	) {
		when (collector) {
			is PlaybackObserver.DurationCollector -> _durationCollectors.sync { remove(collector) }
			is PlaybackObserver.ProgressionCollector -> _progressionCollectors.sync { remove(collector) }
			is PlaybackObserver.QueueCollector -> _queueCollectors.sync { remove(collector) }
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
			observer = this,
			scope = confinedScope,
			playbackConnection = playbackConnection,
		).also {
			sync { if (_disposed) it.dispose() else _durationCollectors.sync { add(it) } }
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
			observer = this,
			scope = confinedScope,
			playbackConnection = playbackConnection,
			collectEvent = includeEvent
		).also {
			sync { if (_disposed) it.dispose() else _progressionCollectors.sync { add(it) } }
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
			observer = this,
			scope = confinedScope,
			playbackConnection = playbackConnection
		).also {
			sync { if (_disposed) it.dispose() else _queueCollectors.sync { add(it) } }
		}
	}

	override fun updateProgress(): Job {
		return parentScope.launch {
			val jobs = mutableListOf<Job>()
			updateProgressRequestListener.removeAll { !it.first.isActive }
			updateProgressRequestListener.forEach {
				it.first.launch { it.second() }.also { job -> jobs.add(job) }
			}
			jobs.joinAll()
		}
	}

	override fun dispose() {
		sync {
			if (_disposed) {
				return
			}
			parentScope.cancel()
			disposeCollectors()
			_disposed = true
		}
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
			for (collector in this) {
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
