package com.kylentt.mediaplayer.domain.mediasession.service

import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.delegates.LateLazy
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import timber.log.Timber
import javax.inject.Singleton



interface MediaServiceSessionListener {
	fun onMediaSessionChanged(mediaSession: MediaSession)
	fun onPlayerSessionChanged(player: Player)
}

/**
 * Class To Manage [MediaSession] of [MediaLibraryService]
 *
 * Typically Only one [MediaSession] will be Initialized per [MediaLibraryService] Lifetime but
 * that might change
 *
 * @param service The [MediaLibraryService]
 */

@Singleton
class MediaServiceSession private constructor(
  private var service: MediaService
) {

	private val sessionChangedListener = mutableListOf<MediaServiceSessionListener>()

  val mediaSession
    get() = service.sessions.last()

  val sessionPlayer
    get() = mediaSession.player

	fun registerSessionChangeListener(listener: MediaServiceSessionListener) {
		sessionChangedListener.add(listener)
	}

	fun unregisterSessionChangeListener(listener: MediaServiceSessionListener): Boolean {
		return sessionChangedListener.remove(listener)
	}

	fun replaceMediaSession(session: MediaSession) {
		if (service.sessions.isNotEmpty()) checkState(service.sessions.size == 1)
		service.addSession(session)
		service.removeSession(service.sessions.first())
		sessionChangedListener.forEach { it.onMediaSessionChanged(mediaSession) }
	}

	fun replaceSessionPlayer(player: Player) {
		if (service.sessions.isNotEmpty()) checkState(service.sessions.size == 1)
		mediaSession.player = player
		sessionChangedListener.forEach { it.onPlayerSessionChanged(sessionPlayer) }
	}

  companion object {
		private val initializer = LateLazy<MediaServiceSession>()
    private val instance by initializer

    fun getInstance(service: MediaService): MediaServiceSession {
      if (!initializer.isInitialized) {
				initializer.initializeValue { MediaServiceSession(service) }
			}
      if (instance.service !== service) {
        instance.service = service
      }
      return instance
    }
  }
}
