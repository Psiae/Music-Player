package com.flammky.musicplayer.playbackcontrol.ui.presenter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface PlaybackObserver {

	fun collectDuration(): StateFlow<Duration>

	fun createProgressionCollector(
		collectorScope: CoroutineScope,
		includeEvent: Boolean = true
	): PlaybackProgressionCollector

	fun updateProgress(): Job

	/**
	 * Dispose the observer, any ongoing job will be cancelled, and no more emission is possible
	 */
	fun dispose()
}


interface PlaybackProgressionCollector {
	val progressStateFlow: StateFlow<Duration>
	val bufferedProgressStateFlow: StateFlow<Duration>

	/**
	 * Start the progress collector
	 *
	 * @return await-able Deferred of initialization value, `StateFlow's` above is guaranteed to be a
	 * valid known value
	 */
	fun startCollectProgressAsync(): Deferred<Unit>

	fun stopCollectProgressAsync(): Deferred<Unit>

	/**
	 * set The IntervalHandler, will cancel current IntervalHandler and [handler] will be called
	 * immediately and then on every elapsed delay.
	 * ** Note: CoroutineContext of `handler` is the internal event loop **
	 *
	 * @param handler the handler lambda, return positive value for a valid delay, negative value to
	 * skip collection until next event, `null` for `next-second` interval
	 *
	 * ** Note: Due to nature of `MediaController.duration` the interval might not be accurate by
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
	fun setCollectEventAsync(
		collectEvent: Boolean /* Array<Event> ? */
	): Deferred<Unit>

	/**
	 * Dispose the collector, supervisor Job will be cancelled
	 */
	fun dispose()
}
