package com.kylentt.mediaplayer.domain.mediasession.libraryservice.state

import android.app.Notification
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.sessions.SessionManager
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.sessions.SessionProvider
import timber.log.Timber

class StateManager(
	private val stateInteractor: MusicLibraryService.StateInteractor
) : MusicLibraryService.ServiceComponent() {
	private val playerListenerImpl = PlayerListenerImpl()

	private val foregroundServiceCondition: (MediaSession) -> Boolean = {
		it.player.playbackState.isOngoing()
	}


	override fun start(componentDelegate: MusicLibraryService.ComponentDelegate) {
		super.start(componentDelegate)
		componentDelegate.sessionInteractor.registerPlayerEventListener(playerListenerImpl)
	}

	override fun release() {
		if (isReleased) return
		if (isStarted) {
			componentDelegate.sessionInteractor.removePlayerEventListener(playerListenerImpl)
		}
		return super.release()
	}

	val interactor = Interactor()


	private inner class PlayerListenerImpl : Player.Listener {
		override fun onPlaybackStateChanged(playbackState: Int) {
			Timber.d("stateManager received onPlaybackStateChanged")

			if (!isStarted || isReleased) return
			val mediaSession = componentDelegate.sessionInteractor.mediaSession ?: return
			val notificationInteractor = componentDelegate.notificationInteractor

			if (foregroundServiceCondition(mediaSession)) {
				if (!stateInteractor.isForeground) {
					notificationInteractor.startForegroundService(mediaSession, onGoingNotification = true)
				}
			} else {
				if (stateInteractor.isForeground) {
					notificationInteractor.stopForegroundService(false)
				}
			}

			when (playbackState) {
				Player.STATE_READY -> {}
				Player.STATE_BUFFERING -> {}
				Player.STATE_IDLE -> {}
				Player.STATE_ENDED -> {}
			}
		}
	}

	inner class Interactor {
		fun startForegroundService(notification: Notification) {
			stateInteractor.startForeground(notification)
		}

		fun stopForegroundService(removeNotification: Boolean) {
			stateInteractor.stopForeground(removeNotification)
		}

		fun startService() {
			stateInteractor.start()
		}

		fun stopService() {
			stateInteractor.stop(false)
		}

		fun releaseService() {
			stateInteractor.release()
		}

		fun getServiceForegroundCondition(mediaSession: MediaSession): Boolean {
			return foregroundServiceCondition(mediaSession)
		}
	}
}
