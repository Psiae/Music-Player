package com.flammky.musicplayer.playbackcontrol.ui.controller

import androidx.annotation.FloatRange
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.time.Duration

internal interface PlaybackController {


	/**
	 * seek to [position] in current playback
	 *
	 * @param position the seek position
	 * @return [RequestResult]
	 */
	fun requestSeekAsync(position: Duration): Deferred<RequestResult>

	/**
	 * seek to [progress] percentage in current playback
	 *
	 * @param [progress] the progress percentage
	 * @return [RequestResult]
	 */
	fun requestSeekAsync(@FloatRange(from = 0.0, to = 1.0) progress: Float): Deferred<RequestResult>

	fun requestSeekAsync(index: Int, startPosition: Duration): Deferred<RequestResult>

	/**
	 * @param success whether the request is successful
	 * @param eventDispatch the event dispatching Job, Job completion denotes that this seek event is
	 * already been dispatched to all observers.
	 * ** non-null if [success] **
	 */
	data class RequestResult(
		val success: Boolean,
		val eventDispatch: Job?
	) {
		init {
			if (success) requireNotNull(eventDispatch) {
				"SeekRequest was successful($success) but eventDispatch($eventDispatch) is null "
			} else require(eventDispatch == null) {
				"SeekRequest was not successful($success) but eventDispatch($eventDispatch) is not null"
			}
		}
	}
}
