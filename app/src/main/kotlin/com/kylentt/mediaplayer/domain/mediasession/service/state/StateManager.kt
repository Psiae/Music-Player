package com.kylentt.mediaplayer.domain.mediasession.service.state

import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import timber.log.Timber

class MusicLibraryStateManager : MusicLibraryService.ServiceComponent() {
	private val playerListenerImpl = PlayerListenerImpl()

	private val foregroundServiceCondition: (MediaSession) -> Boolean = {
		it.player.playbackState.isOngoing()
	}

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
	override fun create(serviceInteractor: MusicLibraryService.ServiceInteractor) {
		super.create(serviceInteractor)
	}

	@MainThread
	override fun start(componentInteractor: MusicLibraryService.ComponentInteractor) {
		super.start(componentInteractor)
		componentInteractor.mediaSessionManagerDelegator.registerPlayerEventListener(playerListenerImpl)
		isStarted = true
	}

	@MainThread
	override fun stop(componentInteractor: MusicLibraryService.ComponentInteractor) {
		super.stop(componentInteractor)
		componentInteractor.mediaSessionManagerDelegator.removePlayerEventListener(playerListenerImpl)
		isStarted = false
	}

	@MainThread
	override fun release(obj: Any) {
		Timber.i("StateManager.release() called by $obj")

		super.release(obj)
		isReleased = true

		Timber.i("StateManager released by $obj")
	}

	private inner class PlayerListenerImpl : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			if (!isStarted) return
			val getServiceInteractor = serviceInteractor!!

			val mediaSession =
				componentInteractor?.mediaSessionManagerDelegator?.mediaSession ?: return
			val notificationManagerDelegator =
				componentInteractor?.mediaNotificationManagerDelegator?: return

			if (foregroundServiceCondition(mediaSession)) {
				if (!getServiceInteractor.isForeground) {
					notificationManagerDelegator
						.startForegroundService(mediaSession, onGoingNotification = true, isEvent = true)
				}
			} else {
				if (getServiceInteractor.isForeground) {
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