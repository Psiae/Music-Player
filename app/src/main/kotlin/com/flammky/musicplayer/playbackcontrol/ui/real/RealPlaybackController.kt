package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.ui.playbackcontrol.RealPlaybackControlPresenter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class RealPlaybackController(
	private val scope: CoroutineScope,
	private val presenter: RealPlaybackControlPresenter,
	private val playbackConnection: PlaybackConnection
) : PlaybackController {

	private val _observers = mutableListOf<PlaybackObserver>()

	override fun dispose() {
		scope.cancel()
	}

	override fun createObserver(): PlaybackObserver {
		return RealPlaybackObserver(
			parentScope = scope,
			playbackConnection = playbackConnection
		).also {
			_observers.sync { add(it) }
		}
	}

	override fun observePlayCommand(): Flow<Boolean> {
		TODO("Not yet implemented")
	}

	override fun observePauseCommand(): Flow<Boolean> {
		TODO("Not yet implemented")
	}

	override fun requestSeekAsync(position: Duration): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.getSession()?.controller?.withContext { seekProgress(position) }
				?: false
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach { jobs.add(it.updateProgress()) }
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(progress: Float): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.getSession()?.controller?.withContext {
				seekProgress((duration.inWholeMilliseconds * progress).toLong().milliseconds)
			} ?: false
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach { jobs.add(it.updateProgress()) }
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(index: Int, startPosition: Duration): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.getSession()?.controller?.withContext {
				seekIndex(index, startPosition)
			} ?: false
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					launch {
						val jobs = mutableListOf<Job>()
						_observers.forEach { jobs.add(it.updateProgress()) }
						jobs.joinAll()
					}
				} else {
					null
				}
			)
		}
	}
}
