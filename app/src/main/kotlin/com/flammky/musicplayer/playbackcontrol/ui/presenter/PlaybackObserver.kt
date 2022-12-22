package com.flammky.musicplayer.playbackcontrol.ui.presenter

import com.flammky.musicplayer.media.playback.PlaybackQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

interface PlaybackObserver {

	val disposed: Boolean

	fun createDurationCollector(
		collectorContext: CoroutineContext = EmptyCoroutineContext,
	): DurationCollector

	fun createProgressionCollector(
		collectorContext: CoroutineContext = EmptyCoroutineContext,
		includeEvent: Boolean = true
	): ProgressionCollector

	fun createQueueCollector(
		collectorContext: CoroutineContext = EmptyCoroutineContext,
	): QueueCollector

	/**
	 * Dispose the observer, any ongoing job will be cancelled, and no more emission is possible
	 */
	fun dispose()

	sealed interface Collector {
		val disposed: Boolean
		fun dispose()
	}

	interface ProgressionCollector : Collector {
		val positionStateFlow: StateFlow<Duration>
		val bufferedPositionStateFlow: StateFlow<Duration>

		/**
		 * Start the progress collector
		 *
		 * @return await-able Deferred of initialization value, `StateFlow's` above is guaranteed to be a
		 * valid known value
		 */
		fun startCollectPosition(): Job

		fun stopCollectProgress(): Job

		/**
		 * set The IntervalHandler, will cancel current IntervalHandler and [handler] will be called
		 * immediately and then on every elapsed delay.
		 * ** Note: CoroutineContext of `handler` is the internal event loop **
		 *
		 * @param handler the handler lambda, return positive value for a valid delay, negative value to
		 * skip collection until next event, `null` for `next-second` interval
		 *
		 * ** Note: Due to nature of `MediaController.position` the interval might not be accurate by
		 * about 10ms, may be both early or late, this is on fix list, which we might just not use it **
		 */
		fun setIntervalHandler(
			handler: suspend (
				isEvent: Boolean,
				progress: Duration,
				duration: Duration,
				speed: Float
			) -> Duration?
		)

		/**
		 * whether to also include `progress` affecting event
		 */
		fun setCollectEvent(
			collectEvent: Boolean /* Array<Event> ? */
		): Job

		/**
		 * Dispose the collector, supervisor Job will be cancelled
		 */
		override fun dispose()
	}

	interface DurationCollector : Collector {
		val durationStateFlow: StateFlow<Duration>
		fun startCollect(): Job
		fun stopObserve(): Job
		override fun dispose()
	}

	interface QueueCollector : Collector {
		/**
		 * The current PlaybackQueue,
		 * [PlaybackQueue.UNSET] if invalid
		 */
		val queueStateFlow: StateFlow<PlaybackQueue>

		/**
		 * start observing
		 * @return the launching Job, normal completion denoting that the request has been completed
		 */
		fun startCollect(): Job
		fun stopCollect(): Job
		override fun dispose()
	}
}
