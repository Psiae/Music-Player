package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackObserver
import kotlinx.coroutines.*

interface PlaybackControlPresenter {

	/**
	 * create a Playback Observer.
	 *
	 * @param owner the observer owner
	 * @param scope the observer parent coroutine scope
	 * ** scope must have `CoroutineDispatcher` with confined parallelism provided, to be removed **
	 * ** cancelling the provided @param scope will also cancel the PlaybackObserver scope **
	 * @param key the key, there will be no duplicate observer within the same owner and key
	 */
	fun observePlayback(
		owner: Any,
		scope: CoroutineScope,
		key: String? = null
		/* should we have key mechanism ?, many problematic things will follow though */
	): PlaybackObserver = throw NotImplementedError()
}


class RealPlaybackControlPresenter(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	private val _owners = mutableMapOf<Any, MutableMap<String?, Any>>()

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun observePlayback(
		owner: Any,
		scope: CoroutineScope,
		key: String?
	): PlaybackObserver {
		val scopeDispatcher = requireNotNull(scope.coroutineContext[CoroutineDispatcher]) {
			"CoroutineScope $scope does Not have `CoroutineDispatcher` provided"
		}
		// realistically speaking will there be such case ?
		val scopeJob = requireNotNull(scope.coroutineContext[Job]) {
			"CoroutineScope $scope does Not have `Job` provided"
		}
		val localScope = try {
			CoroutineScope(context = SupervisorJob(scopeJob) + scopeDispatcher.limitedParallelism(1))
		} catch (uoe: UnsupportedOperationException) {
			error("Unconfined Dispatcher $scopeDispatcher is not allowed")
		}
		return RealPlaybackObserver(
			scope = localScope,
			dispatchers = dispatchers,
			playbackConnection = playbackConnection
		)
	}
}
