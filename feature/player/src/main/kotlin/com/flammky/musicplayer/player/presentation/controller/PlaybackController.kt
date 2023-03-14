package com.flammky.musicplayer.player.presentation.controller

import androidx.annotation.MainThread
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * PlaybackController for sending command / observe
 */
// Note: keep this to `playbackcontrol` package only, made due to the dynamic nature of the ui
// don't stretch this to other module unnecessarily

// consider to limit the command dispatch to the `Main` dispatcher only
internal abstract class PlaybackController(
	// maybe a wrapper object ?
	val user: User
) {

	// TODO: Playback Properties getter

	/**
	 * Disposed state of this controller, if this variable return true any commands or observe request
	 * will be ignored / return an invalid value
	 */
	abstract val disposed: Boolean
		@MainThread
		get

	/**
	 * create a disposable Playback Observer to observe the session playback info
	 *
	 * @param coroutineContext The coroutine context
	 * ** Do note that the dispatcher might be ignored **
	 */
	abstract fun createPlaybackObserver(
		coroutineContext: CoroutineContext = EmptyCoroutineContext,
	): PlaybackObserver

	abstract fun requestSeekPositionAsync(
		expectId: String,
		expectDuration: Duration,
		percent: Float
	): Deferred<RequestResult>

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

	abstract fun requestSeekAsync(
		/* TODO: snapshot */
		index: Int,
		startPosition: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSeekAsync(
		expectFromIndex: Int,
		expectFromId: String,
		expectToIndex: Int,
		expectToId: String,
		startPosition: Duration = Duration.ZERO,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSeekNextAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSeekPreviousAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSeekPreviousItemAsync(
		startPosition: Duration,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSetPlayWhenReadyAsync(
		playWhenReady: Boolean,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestPlayAsync(
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSetRepeatModeAsync(
		repeatMode: RepeatMode,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestSetShuffleModeAsync(
		shuffleMode: ShuffleMode,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): Deferred<RequestResult>

	abstract fun requestToggleRepeatModeAsync(): Deferred<RequestResult>

	abstract fun requestToggleShuffleModeAsync(): Deferred<RequestResult>

	abstract fun requestMoveAsync(
		from: Int,
		expectFromId: String,
		to: Int,
		expectToId: String
	): Deferred<RequestResult>

	/**
	 * Request to do `CompareAndSet` operation directly
	 * `compareAndSet` is executed directly in the controller looper
	 */
	abstract fun requestCompareAndSetAsync(
		compareAndSet: CompareAndSetScope.() -> Unit
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

	// TODO
	interface CompareAndSetScope {
		fun getQueue(): OldPlaybackQueue
		fun setQueue(expect: OldPlaybackQueue, set: OldPlaybackQueue)
	}
}
