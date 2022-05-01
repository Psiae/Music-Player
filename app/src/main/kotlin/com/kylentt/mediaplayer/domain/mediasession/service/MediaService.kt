package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.PendingIntent
import android.content.*
import android.content.pm.ServiceInfo
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Constants.SESSION_ID
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.ALIVE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.DESTROYED
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.FOREGROUND
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.setCurrent
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.updateState
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_NEXT
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PAUSE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PLAY
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PREV
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_STOP_CANCEL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.PLAYBACK_CONTROL_ACTION
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.PLAYBACK_CONTROL_INTENT
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.syncEachClear
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MediaService : MediaLibraryService() {

  init {
    if (!MainActivity.wasLaunched) {
      exitProcess(0)
    }
  }

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var coilHelper: CoilHelper
  @Inject lateinit var dispatchers: AppDispatchers
  @Inject lateinit var exoPlayer: ExoPlayer
  @Inject lateinit var itemHelper: MediaItemHelper
  @Inject lateinit var protoRepo: ProtoRepository
  @Inject lateinit var sessionManager: MediaSessionManager

  lateinit var mediaSession: MediaSession

  private val librarySession by lazy {
    MediaLibrarySession
      .Builder(this, exoPlayer, MediaLibraryCallback())
      .setId(SESSION_ID)
      .setSessionActivity(sessionActivity)
      .setMediaItemFiller(DefaultItemFiller())
      .build()
  }

  private val sessionActivity by lazy {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    PendingIntent.getActivity(this, 444, intent, flag)
  }

  private val notificationProvider by lazy { MediaServiceNotification(service = this) }
  private val playbackReceiver by lazy { PlaybackActionReceiver() }
  val computationScope by lazy { CoroutineScope(dispatchers.computation + SupervisorJob()) }
  val mainScope by lazy { CoroutineScope(dispatchers.main + SupervisorJob()) }
  val ioScope by lazy { CoroutineScope(dispatchers.io + SupervisorJob()) }

  val onDestroyCallback = mutableListOf<() -> Unit>()


  override fun onCreate() {
    super.onCreate()
    setCurrent()
    updateState(ALIVE)
    setMediaNotificationProvider(notificationProvider)
    registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_CONTROL_INTENT))
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = librarySession

  override fun onDestroy() {
    onDestroyCallback.syncEachClear()
    librarySession.release()
    exoPlayer.release()
    cancelServiceScope()
    unregisterReceiver(playbackReceiver)
    return run {
      if (!MainActivity.isAlive) {
        // MediaSessionStub could be Leaking this Service Context Internally
        exitProcess(0)
      }
      super.onDestroy()
      updateState(DESTROYED)
    }
  }

  private fun cancelServiceScope() {
    computationScope.cancel()
    ioScope.cancel()
    mainScope.cancel()
  }

  fun startServiceAsForeground(id: Int, notification: Notification) {
    if (VersionHelper.hasQ()) {
      startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    } else {
      startForeground(id, notification)
    }
    updateState(FOREGROUND)
  }

  fun stopServiceAsForeground(removeNotification: Boolean) {
    stopForeground(removeNotification)
    updateState(ALIVE)
  }

  inner class PlaybackActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.getStringExtra(PLAYBACK_CONTROL_ACTION)) {
        ACTION_PLAY -> {
          if (exoPlayer.playbackState.isStateEnded()) {
            if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem()
            exoPlayer.seekTo(0)
          }
          sessionManager.sendControllerCommand(ControllerCommand.PREPARE)
          sessionManager.sendControllerCommand(ControllerCommand.SetPlayWhenReady(true))
        }
        ACTION_PAUSE -> {
          sessionManager.sendControllerCommand(ControllerCommand.SetPlayWhenReady(false))
        }
        ACTION_NEXT -> {
          sessionManager.sendControllerCommand(ControllerCommand.SeekToNextItem)
        }
        ACTION_PREV -> {
          sessionManager.sendControllerCommand(ControllerCommand.SeekToPrevItem)
        }
        ACTION_REPEAT_OFF_TO_ONE -> {
          val mode = Player.REPEAT_MODE_ONE
          sessionManager.sendControllerCommand(ControllerCommand.SetRepeatMode(mode))
        }
        ACTION_REPEAT_ONE_TO_ALL -> {
          val mode = Player.REPEAT_MODE_ALL
          sessionManager.sendControllerCommand(ControllerCommand.SetRepeatMode(mode))
        }
        ACTION_REPEAT_ALL_TO_OFF -> {
          val mode = Player.REPEAT_MODE_OFF
          sessionManager.sendControllerCommand(ControllerCommand.SetRepeatMode(mode))
        }
        ACTION_STOP_CANCEL -> {
          val duration = 1000L // App Settings
          val command = ControllerCommand
            .WithFadeOut(ControllerCommand.STOP,true, duration, 50L, 0F) {
              stopServiceAsForeground(true)
            }
          sessionManager.sendControllerCommand(command)

        }

      }
    }
  }

  inner class MediaLibraryCallback : MediaLibrarySession.MediaLibrarySessionCallback {
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
      mediaSession = session
      return super.onConnect(session, controller)
    }
  }

  inner class DefaultItemFiller() : MediaSession.MediaItemFiller {
    override fun fillInLocalConfiguration(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItem: MediaItem
    ): MediaItem = itemHelper.rebuildMediaItem(mediaItem)
  }

  companion object {
    @JvmStatic val wasLaunched
      get() = Lifecycle.wasLaunched
    @JvmStatic val isAlive
      get() = Lifecycle.isAlive
    @JvmStatic val isDestroyed
      get() = Lifecycle.isDestroyed
    @JvmStatic val isForeground
      get() = Lifecycle.isForeground

    @JvmStatic
    fun getComponentName(context: Context) = ComponentName(context, MediaService::class.java)

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
      @IntDef(NOTHING, DESTROYED, ALIVE, FOREGROUND)
      private annotation class ServiceState

      const val NOTHING = 0
      const val DESTROYED = 1
      const val ALIVE = 2
      const val FOREGROUND = 3

      private var currentServiceHash = NOTHING
      private var currentServiceState = NOTHING

      val wasLaunched
        get() = currentServiceHash != NOTHING
          && currentServiceState != NOTHING
      val isAlive
        get() = currentServiceState >= ALIVE
      val isDestroyed
        get() = currentServiceState == DESTROYED
      val isForeground
        get() = currentServiceState == FOREGROUND

      @MainThread
      fun MediaService.setCurrent() {
        verifyMainThread()
        currentServiceHash = this.hashCode()
      }

      @MainThread
      fun MediaService.updateState(@ServiceState state: Int) {
        verifyMainThread()
        require(state in (NOTHING + 1)..FOREGROUND)
        require(currentHashEqual())
        currentServiceState = state
      }

      private fun MediaService.currentHashEqual(): Boolean {
        val thisHash = this.hashCode()
        val thatHash = currentServiceHash
        return thisHash == thatHash
      }
    }

    object Constants {
      const val SESSION_ID = "kylentt"
    }
  }
}
