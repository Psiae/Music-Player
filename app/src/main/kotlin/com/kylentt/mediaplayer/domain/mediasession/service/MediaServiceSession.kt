package com.kylentt.mediaplayer.domain.mediasession.service

import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.delegates.Synchronize
import timber.log.Timber
import javax.inject.Singleton

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

  private val listeners = mutableListOf<Pair<Player, Player.Listener>>()

  val mediaSession
    get() = service.sessions.last()

  val sessionPlayer
    get() = mediaSession.player

  fun registerListener(player: Player, listener: Player.Listener) {
    player.addListener(listener)
    listeners.add(player to listener)
  }

  fun unregisterListener(player: Player) {
    var counter = 0
    listeners.forEach {
      if (it.first === player) {
        it.first.removeListener(it.second)
        val msg =
          "MediaServiceSession unregisterListenerByPlayer," +
          "\nremoved ${it.second} from ${it.first}, counter: ${++counter}"
        Timber.d(msg)
      }
    }
  }

  fun unregisterListener(listener: Player.Listener) {
    var counter = 0
    listeners.forEach {
      if (it.second === listener) {
        it.first.removeListener(it.second)
        val msg =
          "MediaServiceSession unregisterListenerByListener," +
            "\nremoved ${it.second} from ${it.first}, counter: ${++counter}"
        Timber.d(msg)
      }
    }
  }

  fun unregisterListener(player: Player, listener: Player.Listener) {
    var counter = 0
    listeners.forEach {
      if (it.first === player && it.second === listener) {
        it.first.removeListener(it.second)
        val msg =
          "MediaServiceSession unregisterListenerByPair," +
            "\nremoved ${it.second} from ${it.first}, counter: ${++counter}"
        Timber.d(msg)
      }
    }
  }

  fun unregisterAllListener() {
    listeners.forEach { it.first.removeListener(it.second) }
  }

  fun releaseSessionPlayer() {
    unregisterListener(sessionPlayer)
    sessionPlayer.release()
  }

  fun replaceSessionPlayer(player: Player, removeListener: Boolean, release: Boolean) {
    if (removeListener) {
      unregisterListener(sessionPlayer)
    }
    if (release) {
      releaseSessionPlayer()
    }
    mediaSession.player = player
  }

  fun release() {
    unregisterAllListener()
  }

  companion object {
    val EMPTY = MediaServiceSession(MediaService())

    private var instance by Synchronize(EMPTY)

    fun getInstance(service: MediaService): MediaServiceSession {
      if (instance.service !== service) instance.release()
      instance.service = service
      return instance
    }
  }
}
