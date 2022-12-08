package com.flammky.musicplayer.playbackcontrol.ui.real

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.ui.playbackcontrol.RealPlaybackControlPresenter
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class RealPlaybackController(
	private val scope: CoroutineScope,
	private val presenter: RealPlaybackControlPresenter,
	private val playbackConnection: PlaybackConnection
) : PlaybackController {

	override fun requestSeekAsync(position: Duration): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.withControllerContext { seekProgress(position) }
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch { presenter.notifySeekRequest().joinAll() }
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(progress: Float): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.withControllerContext {
				seekProgress((getDuration().inWholeMilliseconds * progress).toLong().milliseconds)
			}
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch { presenter.notifySeekRequest().joinAll() }
				} else {
					null
				}
			)
		}
	}

	override fun requestSeekAsync(index: Int, startPosition: Duration): Deferred<PlaybackController.RequestResult> {
		return scope.async {
			val success = playbackConnection.withControllerContext {
				seekIndex(index, startPosition)
			}
			PlaybackController.RequestResult(
				success = success,
				eventDispatch = if (success) {
					scope.launch { presenter.notifySeekRequest().joinAll() }
				} else {
					null
				}
			)
		}
	}
}
