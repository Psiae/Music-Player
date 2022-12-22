package com.flammky.musicplayer.playbackcontrol.ui.controller

import androidx.annotation.FloatRange
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * PlaybackController for sending command / observe @param [sessionID]
 * @param sessionID the session ID
 */
internal abstract class PlaybackController(
	/* auth: AuthContext */
	val sessionID: String
) {

	// TODO: Playback Properties getter

	/**
	 * Disposed state of this controller, if this variable return true any commands or observe request
	 * will be ignored / return an invalid value
	 */
	abstract val disposed: Boolean

	/**
	 * create a disposable Playback Observer to observe the session playback info
	 */
	abstract fun createPlaybackObserver(
		coroutineContext: CoroutineContext = EmptyCoroutineContext,
	): PlaybackObserver

	/**
	 * seek to [position] in current playback
	 *
	 * @param position the seek position
	 * @return [RequestResult]
	 */
	abstract fun requestSeekAsync(
		position: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	/**
	 * seek to [progress] percentage in current playback
	 *
	 * @param [progress] the progress percentage
	 * @return [RequestResult]
	 */
	abstract fun requestSeekAsync(@FloatRange(from = 0.0, to = 1.0) progress: Float): Deferred<RequestResult>

	abstract fun requestSeekAsync(
		index: Int,
		startPosition: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun dispose()

	/**
	 * @param success whether the request is successful
	 * @param eventDispatch the event dispatching Job, Job completion denotes that this event
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
