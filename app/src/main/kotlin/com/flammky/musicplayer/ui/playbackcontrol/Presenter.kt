package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackObserver
import kotlinx.coroutines.*

interface PlaybackControlPresenter {

	/**
	 * create a Playback Observer
	 */
	fun observePlayback(
		owner: Any,
		scope: CoroutineScope
	): PlaybackObserver = throw NotImplementedError()
}


class RealPlaybackControlPresenter(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val playbackConnection: PlaybackConnection
): PlaybackControlPresenter {

	@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
	override fun observePlayback(
		owner: Any,
		scope: CoroutineScope
	): PlaybackObserver {
		val dispatcher = requireNotNull(scope.coroutineContext[CoroutineDispatcher]) {
			"CoroutineScope $scope does Not have `CoroutineDispatcher` provided"
		}
		val job = requireNotNull(scope.coroutineContext[Job]) {
			"CoroutineScope $scope does Not have `Job` provided"
		}
		val localScope = CoroutineScope(
			context = SupervisorJob(job) + dispatcher.limitedParallelism(1)
		)
		return RealPlaybackObserver(
			scope = localScope,
			dispatchers = dispatchers,
			playbackConnection = playbackConnection
		)
	}
}
