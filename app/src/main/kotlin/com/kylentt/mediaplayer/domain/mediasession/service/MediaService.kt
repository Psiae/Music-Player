package com.kylentt.mediaplayer.domain.mediasession.service

import androidx.annotation.IntDef
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlin.system.exitProcess

interface ExposeServiceState {
  fun setCurrent()
  fun alive()
  fun destroyed()
}

@AndroidEntryPoint
class MediaService : MediaLibraryService(), ExposeServiceState {

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var dispatchers: AppDispatchers
  @Inject lateinit var exoPlayer: ExoPlayer
  @Inject lateinit var protoRepo: ProtoRepository
  /*@Inject lateinit var serviceConnector: MediaServiceConnector*/

  private lateinit var _librarySession: MediaLibrarySession
  private val librarySession: MediaLibrarySession
    get() {
      if (::_librarySession.isInitialized) return _librarySession
      _librarySession = MediaLibrarySession
        .Builder(this, exoPlayer, MediaLibraryCallback()).build()
      return _librarySession
    }

  val serviceScope = CoroutineScope(dispatchers.main + SupervisorJob())


  init {
    if (!MainActivity.wasLaunched) {
      // TODO
      exitProcess(0)
    }
  }

  override fun onCreate() {
    super.onCreate()
    val notificationProvider =
      MediaServiceNotification(
        dispatchers = dispatchers,
        protoRepo = protoRepo,
        service = this,
        scope = appScope
      )
    setMediaNotificationProvider(notificationProvider)
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
    return librarySession
  }

  inner class MediaLibraryCallback : MediaLibrarySession.MediaLibrarySessionCallback {
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
      return super.onConnect(session, controller)
    }
  }

  override fun onDestroy() {
    librarySession.release()
    exoPlayer.release()
    return super.onDestroy()
  }

  override fun setCurrent() {
    TODO("Not yet implemented")
  }

  override fun alive() {
    TODO("Not yet implemented")
  }

  override fun destroyed() {
    TODO("Not yet implemented")
  }

  companion object {
    val wasLaunched
      get() = Lifecycle.wasLaunched
    val isAlive
      get() = Lifecycle.isAlive
    val isDestroyed
      get() = Lifecycle.isDestroyed
    val isForeground
      get() = Lifecycle.isForeground

    private object Lifecycle {
      @Retention(AnnotationRetention.SOURCE)
      @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.LOCAL_VARIABLE,
      )
      @IntDef(nothing, destroyed, alive, foreground)
      private annotation class ServiceState

      const val nothing = 0
      const val destroyed = 1
      const val alive = 2
      const val foreground = 3

      private var currentServiceHash = nothing
      private var currentServiceState = nothing

      val wasLaunched
        get() = currentServiceHash != nothing
          && currentServiceState != nothing

      val isAlive
        get() = currentServiceState >= alive
      val isDestroyed
        get() = !isAlive && wasLaunched
      val isForeground
        get() = currentServiceState >= foreground

      private fun MediaService.setCurrent() {
        currentServiceHash = this.hashCode()
      }

      private fun MediaService.checkCurrentHash(): Boolean {
        val thisHash = this.hashCode()
        val thatHash = currentServiceHash
        return thisHash == thatHash
      }

      private fun MediaService.setState(@ServiceState int: Int) {
        check(checkCurrentHash())
        currentServiceState = int
      }
    }
  }
}
