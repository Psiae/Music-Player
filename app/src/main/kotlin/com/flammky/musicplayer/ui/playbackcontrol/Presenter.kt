package com.flammky.musicplayer.ui.playbackcontrol

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.playbackcontrol.ui.real.RealPlaybackObserver
import kotlinx.coroutines.CoroutineScope

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

	override fun observePlayback(
		owner: Any,
		scope: CoroutineScope
	): PlaybackObserver {
		return RealPlaybackObserver(scope, dispatchers, playbackConnection)
	}
}
