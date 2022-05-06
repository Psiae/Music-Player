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
import com.kylentt.mediaplayer.app.delegates.Synchronize
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Constants.SESSION_ID
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.ALIVE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.DESTROYED
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.FOREGROUND
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.setCurrent
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.updateState
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_DISMISS_NOTIFICATION
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_NEXT
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PAUSE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PLAY
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_PREV
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.ACTION_STOP_CANCEL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.NOTIFICATION_ID
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.PLAYBACK_CONTROL_ACTION
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotification.Companion.PLAYBACK_CONTROL_INTENT
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand.Companion.wrapWithFadeOut
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MediaService : MediaLibraryService() {

  /**
   * Validate Initialization of this Service
   * Internal Binder might launch this Service when it shouldn't for several reason
   */

  private fun validateCreation() {
    if (!MainActivity.wasLaunched) {
      stopForeground(true)
      exitProcess(0)
    }
  }

  /**
   * [MediaSession] Manager for this Service
   *
   * Initialized on [MediaLibraryCallback.onConnect]
   */

  private var serviceSession by Synchronize(MediaServiceSession.EMPTY)
  private val onSessionReady = mutableListOf<(MediaServiceSession) -> Unit>()

  @MainThread
  fun whenSessionReady(what: (MediaServiceSession) -> Unit) {
    checkMainThread()
    if (serviceSession != MediaServiceSession.EMPTY) {
      return what(serviceSession)
    }
    onSessionReady.add { what(it) }
  }

  val currentMediaSession
    get() = serviceSession.mediaSession

  val sessionPlayer
    get() = serviceSession.sessionPlayer

  /**
   * this Service Component Dependency
   * initialized on [MediaService_MembersInjector]
   */

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var coilHelper: CoilHelper
  @Inject lateinit var dispatchers: AppDispatchers
  @Inject lateinit var exoPlayer: ExoPlayer
  @Inject lateinit var itemHelper: MediaItemHelper
  @Inject lateinit var protoRepo: ProtoRepository
  @Inject lateinit var sessionManager: MediaSessionManager


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
    validateCreation()
    super.onCreate()
    registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_CONTROL_INTENT))
    setMediaNotificationProvider(notificationProvider)
    setCurrent()
    updateState(ALIVE)
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = librarySession

  override fun onDestroy() {
    onDestroyCallback.forEachClear()
    librarySession.release()
    cancelServiceScope()
    releaseAllPlayers()
    unregisterReceivers()
    updateState(DESTROYED)
    super.onDestroy()
    if (!MainActivity.isAlive) {
      // MediaLibraryServiceImpl in MediaSessionStub could be Leaking this Service Context Internally
      val msg =
        "MediaService released because !MainActivity.isAlive, " +
        "\ncurrent: ${MainActivity.stateString}, " +
        "\nexiting Process with Status: 0"
      Timber.d(msg)
      exitProcess(0)
    }
  }

  private fun cancelServiceScope() {
    computationScope.cancel()
    ioScope.cancel()
    mainScope.cancel()
  }

  private fun releaseAllPlayers() {
    if (exoPlayer !== sessionPlayer) {
      exoPlayer.release()
    }
    serviceSession.releaseSessionPlayer()
  }

  private fun unregisterReceivers() {
    unregisterReceiver(playbackReceiver)
  }

  @MainThread
  fun startServiceAsForeground(id: Int, notification: Notification) {
    checkMainThread()
    if (VersionHelper.hasQ()) {
      startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    } else {
      startForeground(id, notification)
    }
    updateState(FOREGROUND)
  }

  @MainThread
  fun stopServiceFromForeground(removeNotification: Boolean) {
    checkMainThread()
    stopForeground(false)
    if (removeNotification) {
      notificationProvider.notificationManager.cancel(NOTIFICATION_ID)
    }
    updateState(ALIVE)
  }

  private inner class PlaybackActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.getStringExtra(PLAYBACK_CONTROL_ACTION)

      Timber.d("onReceive, action: $action, playbackState: ${sessionPlayer.playbackState}")

      when (action) {
        ACTION_PLAY -> onReceiveActionPlay()
        ACTION_PAUSE -> onReceiveActionPause()
        ACTION_NEXT -> onReceiveActionNext()
        ACTION_PREV -> onReceiveActionPrev()
        ACTION_REPEAT_OFF_TO_ONE -> onReceiveRepeatOffToOne()
        ACTION_REPEAT_ONE_TO_ALL -> onReceiveRepeatOneToAll()
        ACTION_REPEAT_ALL_TO_OFF -> onReceiveRepeatAllToOff()
        ACTION_STOP_CANCEL -> onReceiveStopCancel()
        ACTION_DISMISS_NOTIFICATION -> stopServiceFromForeground(true)
      }
    }

    private fun onReceiveActionPlay() = with(sessionPlayer) {
      when {
        playbackState.isStateEnded() -> {
          if (hasNextMediaItem()) seekToNextMediaItem()
          seekTo(0L)
        }
        playbackState.isStateIdle() -> {
          sessionPlayer.prepare()
        }
      }
      if (playWhenReady) {
        return notificationProvider.updateNotification(serviceSession.mediaSession)
      }
      playWhenReady = true
    }

    private fun onReceiveActionPause() = with(sessionPlayer) {
      if (!playWhenReady) {
        return notificationProvider.updateNotification(serviceSession.mediaSession)
      }
      try {
        checkState(playbackState.isOngoing())
        playWhenReady = false
      } catch (e: IllegalStateException) {
        val shown = true
        val actual = playbackState.isOngoing()
        Timber.e("$e, Inconsistent Notification ActionPause, shown as: $shown, actual: $actual")
      }
    }

    private fun onReceiveActionNext() = with(sessionPlayer) {
      try {
        checkState(hasNextMediaItem())
        seekToNextMediaItem()
      } catch (e: IllegalStateException) {
        notificationProvider.updateNotification(serviceSession.mediaSession)
        val shown = true
        val actual = hasNextMediaItem()
        Timber.e("$e, Inconsistent Notification ActionNext, shown as: $shown, actual: $actual")
      }
    }

    private fun onReceiveActionPrev() = with(sessionPlayer) {
      if (hasPreviousMediaItem()) {
        seekToPreviousMediaItem()
      } else {
        seekTo(0L)
      }
    }

    private fun onReceiveRepeatOffToOne() = with(sessionPlayer) {
      try {
        checkState(repeatMode.isRepeatOff())
        repeatMode = Player.REPEAT_MODE_ONE
      } catch (e: IllegalStateException) {
        notificationProvider.updateNotification(serviceSession.mediaSession)
        val shown = Player.REPEAT_MODE_OFF
        val actual = repeatMode
        Timber.e("$e, Inconsistent Notification RepeatMode, shown as: $shown, actual: $actual")
      }
    }

    private fun onReceiveRepeatOneToAll() = with(sessionPlayer) {
      try {
        checkState(repeatMode.isRepeatOne())
        repeatMode = Player.REPEAT_MODE_ALL
      } catch (e: IllegalStateException) {
        notificationProvider.updateNotification(serviceSession.mediaSession)
        val shown = Player.REPEAT_MODE_ONE
        val actual = repeatMode
        Timber.e("$e, Inconsistent Notification RepeatMode, shown as: $shown, actual: $actual")
      }
    }

    private fun onReceiveRepeatAllToOff() = with(sessionPlayer) {
      try {
        checkState(repeatMode.isRepeatAll())
        repeatMode = Player.REPEAT_MODE_OFF
      } catch (e: IllegalStateException) {
        notificationProvider.updateNotification(serviceSession.mediaSession)
        val shown = Player.REPEAT_MODE_ALL
        val actual = repeatMode
        Timber.e("$e, Inconsistent Notification RepeatMode, shown as: $shown, actual: $actual")
      }
    }

    private fun onReceiveStopCancel() = with(sessionPlayer) {
      val duration = 1000L
      val command = ControllerCommand.STOP.wrapWithFadeOut(flush = true, duration = duration) {
        val foreground = isForeground
        stopServiceFromForeground(true)
        if (!foreground && !MainActivity.isAlive) {
          // NPE in MediaControllerImplBase.java:3041 when calling librarySession.release()
          onDestroy()
        }
      }
      sessionManager.sendControllerCommand(command)
    }
  }

  inner class MediaLibraryCallback : MediaLibrarySession.MediaLibrarySessionCallback {

    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
      // Any Exception Here will not throw, Log.w is used internally
      Timber.d("MediaLibraryCallback onConnect for $session")
      serviceSession = MediaServiceSession.getInstance(this@MediaService)
      onSessionReady.forEachClear { it(serviceSession) }
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

      fun toServiceStateString(@ServiceState state: Int): String {
        return when (state) {
          NOTHING -> "Nothing"
          DESTROYED -> "Destroyed"
          ALIVE -> "Alive"
          FOREGROUND -> "Foreground"
          else -> "INVALID"
        }
      }

      @MainThread
      fun MediaService.setCurrent() {
        checkMainThread()
        currentServiceHash = this.hashCode()
      }

      @MainThread
      fun MediaService.updateState(@ServiceState state: Int) {
        checkMainThread()
        checkArgument(state in (NOTHING + 1)..FOREGROUND)
        checkState(currentHashEqual())
        val current = currentServiceState
        currentServiceState = state
        Timber.d("currentServiceState changed " +
          "\nfrom: ${toServiceStateString(current)}" +
          "\nto: ${toServiceStateString(state)}"
        )
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
