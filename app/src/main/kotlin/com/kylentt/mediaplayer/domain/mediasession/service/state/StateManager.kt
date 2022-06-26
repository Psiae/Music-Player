package com.kylentt.mediaplayer.domain.mediasession.service.state

import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import timber.log.Timber

class MusicLibraryStateManager(
	private val musicLibrary: MusicLibraryService,
	private val stateInteractor: MusicLibraryService.StateInteractor,
) {
	private val playerListenerImpl = PlayerListenerImpl()

	private val foregroundServiceCondition: (MediaSession) -> Boolean = {
		it.player.playbackState.isOngoing()
	}

	private var componentInteractor: MusicLibraryService.ComponentInteractor? = null

	var isReleased: Boolean = false
		private set(value) {
			checkArgument(value) {
				"cannot unRelease this class"
			}
			field = value
		}

	var isStarted: Boolean = false
		private set

	@MainThread
	fun start(componentInteractor: MusicLibraryService.ComponentInteractor) {
		checkMainThread()

		if (isReleased || isStarted) return

		if (this.componentInteractor !== componentInteractor) {
			this.componentInteractor = componentInteractor

			with(this.componentInteractor!!) {
				mediaSessionManagerDelegator.registerPlayerEventListener(playerListenerImpl)
			}
		}

		isStarted = true
	}

	@MainThread
	fun stop() {
		checkMainThread()
		if (isReleased || !isStarted) return

		with(this.componentInteractor!!) {
			mediaSessionManagerDelegator.removePlayerEventListener(playerListenerImpl)
		}

		isStarted = false
	}

	@MainThread
	fun release(obj: Any) {
		Timber.i("StateManager.release() called by $obj")

		checkMainThread()

		if (isReleased) return
		if (isStarted) stop()

		isReleased = true

		Timber.i("StateManager released by $obj")
	}

	private inner class PlayerListenerImpl : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			if (isReleased) return

			val mediaSession =
				componentInteractor?.mediaSessionManagerDelegator?.mediaSession ?: return
			val notificationManagerDelegator =
				componentInteractor?.mediaNotificationManagerDelegator?: return

			if (foregroundServiceCondition(mediaSession)) {
				if (musicLibrary.isServiceStopped) stateInteractor.callStart()
				if (!musicLibrary.isServiceForeground) {
					notificationManagerDelegator
						.startForegroundService(mediaSession, onGoingNotification = true, isEvent = true)
				}
			} else {
				if (musicLibrary.isServiceForeground) {
					notificationManagerDelegator.stopForegroundService(true)
				}
			}

			when(playbackState) {
				Player.STATE_READY -> {}
				Player.STATE_BUFFERING -> {}
				Player.STATE_IDLE -> {}
				Player.STATE_ENDED -> {}
			}
		}
	}

	inner class Delegate {
		fun getServiceForegroundCondition(mediaSession: MediaSession): Boolean {
			return foregroundServiceCondition(mediaSession)
		}
	}
}
