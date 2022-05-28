package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.NotificationManager
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
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.coroutines.AppScope
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Constants.SESSION_ID
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.ALIVE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.DESTROYED
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.FOREGROUND
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.setCurrent
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService.Companion.Lifecycle.updateState
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_DISMISS_NOTIFICATION
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_NEXT
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_PAUSE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_PLAY
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_PREV
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.ACTION_STOP_CANCEL
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.PLAYBACK_CONTROL_ACTION
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.PLAYBACK_CONTROL_INTENT
import com.kylentt.mediaplayer.domain.mediasession.service.MediaServiceNotificationProviderImpl.Companion.PLAYBACK_ITEM_ID
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand.Companion.wrapWithFadeOut
import com.kylentt.mediaplayer.domain.mediasession.service.connector.MediaServiceState
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

interface MediaServiceInterface {
	val currentMediaSession: MediaSession
	val currentSessionPlayer: Player

	val notificationManager: NotificationManager
	val notificationProvider: MediaServiceNotificationProvider

	val serviceListener: MediaServiceListener

	@MainThread
	fun whenSessionReady(what: (MediaServiceSession) -> Unit)

	@MainThread
	fun startServiceAsForeground(notification: Notification)

	@MainThread
	fun stopServiceAsForeground(removeNotification: Boolean)

	@MainThread
	fun registerOnDestroyCallback(what: () -> Unit)
}

@AndroidEntryPoint
class MediaService : MediaLibraryService(), MediaServiceInterface {

  /**
   * [MediaSession] Manager for this Service
   *
   * Initialized on [MediaLibraryCallback.onConnect]
   */

  private lateinit var serviceSession: MediaServiceSession
  private val whenSessionReadyListener = mutableListOf<(MediaServiceSession) -> Unit>()

  @MainThread
  override fun whenSessionReady(what: (MediaServiceSession) -> Unit) {
    checkMainThread()
    if (::serviceSession.isInitialized) {
      return what(serviceSession)
    }
    whenSessionReadyListener.add { what(it) }
  }

  override val currentMediaSession
    get() = serviceSession.mediaSession

  override val currentSessionPlayer
    get() = serviceSession.sessionPlayer

	override val notificationProvider: MediaServiceNotificationProvider by lazy {
		MediaServiceNotificationProviderImpl(service = this)
	}

	override val notificationManager by lazy {
		getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	override val serviceListener: MediaServiceListener by lazy {
		MediaServiceListenerImpl(this)
	}

  /**
   * this Service Component Dependency
   * initialized on [MediaService_MembersInjector]
   */

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var coilHelper: CoilHelper
  @Inject lateinit var dispatchers: AppDispatchers
  @Inject lateinit var exoPlayer: ExoPlayer
  @Inject lateinit var itemHelper: MediaItemHelper
  @Inject lateinit var mediaRepo: MediaRepository
  @Inject lateinit var protoRepo: ProtoRepository
  @Inject lateinit var sessionManager: MediaSessionManager

  private val playbackReceiver = PlaybackActionReceiver()

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

	private val onDestroyCallback = mutableListOf<() -> Unit>()

	val sessionEventHandler: MediaServiceEventHandler by lazy {
		MediaServiceEventHandlerImpl(this)
	}

  val computationScope by lazy { CoroutineScope(dispatchers.computation + SupervisorJob()) }
  val mainScope by lazy { CoroutineScope(dispatchers.main + SupervisorJob()) }
  val ioScope by lazy { CoroutineScope(dispatchers.io + SupervisorJob()) }

  override fun onCreate() {
    super.onCreate()
    if (sessionManager.serviceState.value is MediaServiceState.NOTHING) {
      sessionManager.connectService()
    }
    registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_CONTROL_INTENT))
    setMediaNotificationProvider(notificationProvider)
    setCurrent()
    updateState(ALIVE)

		whenSessionReady {
			serviceListener.registerPlayerListener(it.sessionPlayer,
				serviceListener.defaultPlayerLister
			)
		}
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = librarySession

  override fun onDestroy() {
    onDestroyCallback.forEachClear()
    cancelServiceScope()
    releaseAllPlayers()
    unregisterReceivers()
		librarySession.release()
    super.onDestroy()
		updateState(DESTROYED)
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

	override fun registerOnDestroyCallback(what: () -> Unit) {
		onDestroyCallback.add(what)
	}

  private fun cancelServiceScope() {
    computationScope.cancel()
    ioScope.cancel()
    mainScope.cancel()
  }

  private fun releaseAllPlayers() {
    if (exoPlayer !== currentSessionPlayer) {
      exoPlayer.release()
    }
		currentSessionPlayer.release()
    serviceListener.unregisterAllListener(this)
  }

  private fun unregisterReceivers() {
    unregisterReceiver(playbackReceiver)
  }

  @MainThread
  override fun startServiceAsForeground(notification: Notification) {
    checkMainThread()
    if (VersionHelper.hasQ()) {
      startForeground(notificationProvider.mediaNotificationId,
				notification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
			)
    } else {
      startForeground(notificationProvider.mediaNotificationId, notification)
    }
    updateState(FOREGROUND)
  }

  @MainThread
  override fun stopServiceAsForeground(removeNotification: Boolean) {
    checkMainThread()
    stopForeground(false)
    if (removeNotification) {
      notificationManager.cancel(notificationProvider.mediaNotificationId)
    }
    updateState(ALIVE)
  }

  private inner class PlaybackActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent == null) return
      val action = intent.getStringExtra(PLAYBACK_CONTROL_ACTION) ?: return
      Timber.d("onReceive, action: $action, playbackState: ${currentSessionPlayer.playbackState}")

      if (currentSessionPlayer.currentMediaItem == null) {
        validateItem(intent)
        return
      }

      when (action) {
        ACTION_PLAY -> onReceiveActionPlay(intent)
        ACTION_PAUSE -> onReceiveActionPause()
        ACTION_NEXT -> onReceiveActionNext()
        ACTION_PREV -> onReceiveActionPrev()
        ACTION_REPEAT_OFF_TO_ONE -> onReceiveRepeatOffToOne()
        ACTION_REPEAT_ONE_TO_ALL -> onReceiveRepeatOneToAll()
        ACTION_REPEAT_ALL_TO_OFF -> onReceiveRepeatAllToOff()
        ACTION_STOP_CANCEL -> onReceiveStopCancel()
        ACTION_DISMISS_NOTIFICATION -> stopServiceAsForeground(true)
      }
    }

    private var isValidating = false
    private fun validateItem(intent: Intent?) = with(currentSessionPlayer) {
      Timber.d("checkItem CurrentMediaItem: $currentMediaItem")

      if (currentMediaItem == null && !isValidating) {
        isValidating = true

        val mediaId = intent?.getStringExtra(PLAYBACK_ITEM_ID)
        when {
          mediaId == null -> Unit
          mediaId.startsWith(MediaStoreSong.MEDIA_ID_PREFIX) -> {
            mainScope.launch {
              val item = mediaRepo.getMediaStoreSongById(mediaId)
              item?.let { setMediaItem(it.asMediaItem) }
              isValidating = false
            }
          }
        }
      }
    }

    private fun onReceiveActionPlay(intent: Intent?): Unit = with(currentSessionPlayer) {
			if (playWhenReady && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}

      when {
        playbackState.isStateEnded() -> {
          if (hasNextMediaItem()) seekToNextMediaItem()
          seekTo(0L)
        }
        playbackState.isStateIdle() -> {
          currentSessionPlayer.prepare()
        }
      }

      playWhenReady = true
    }

    private fun onReceiveActionPause() = with(currentSessionPlayer) {
      if (!playWhenReady && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
      }
			try {
				checkState(playbackState.isOngoing())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Pause received when $this is Idle / Ended")
			} finally {
				playWhenReady = false
			}
    }

    private fun onReceiveActionNext() = with(currentSessionPlayer) {
			if (!hasNextMediaItem() && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}
      try {
        checkState(hasNextMediaItem())
      } catch (e: IllegalStateException) {
				Timber.e("$e: Notification Next received when $this has no Next Item")
      } finally {
				seekToNextMediaItem()
      }
    }

    private fun onReceiveActionPrev() = with(currentSessionPlayer) {
			if (!hasPreviousMediaItem() && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}
			try {
				checkState(hasPreviousMediaItem())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Previous received when $this has no Previous Item")
			} finally {
				seekToPreviousMediaItem()
			}
    }

    private fun onReceiveRepeatOffToOne() = with(currentSessionPlayer) {
			if (!repeatMode.isRepeatOff() && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}
      try {
        checkState(repeatMode.isRepeatOff())
      } catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOne received when $this Repeat is not Off")
      } finally {
				repeatMode = Player.REPEAT_MODE_ONE
      }
    }

    private fun onReceiveRepeatOneToAll() = with(currentSessionPlayer) {
			if (!repeatMode.isRepeatOne() && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}
			try {
				checkState(repeatMode.isRepeatOne())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToAll received when $this Repeat is not One")
			} finally {
				repeatMode = Player.REPEAT_MODE_ALL
			}
    }

    private fun onReceiveRepeatAllToOff() = with(currentSessionPlayer) {
			if (!repeatMode.isRepeatAll() && playbackState.isOngoing()) {
				return notificationProvider.updateSessionNotification(serviceSession.mediaSession)
			}
			try {
				checkState(repeatMode.isRepeatAll())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOff received when $this Repeat is not All")
			} finally {
				repeatMode = Player.REPEAT_MODE_OFF
			}
    }

    private fun onReceiveStopCancel() = with(currentSessionPlayer) {
      val duration = 1000L
			val removeNotification = !playbackState.isOngoing()
      val command = ControllerCommand.STOP.wrapWithFadeOut(flush = true, duration = duration) {
        val foreground = isForeground
				stopServiceAsForeground(removeNotification)
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
      whenSessionReadyListener.forEachClear { it(serviceSession) }
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
    @JvmStatic
		val wasLaunched
      get() = Lifecycle.wasLaunched
    @JvmStatic
		val isAlive
      get() = Lifecycle.isAlive
    @JvmStatic
		val isDestroyed
      get() = Lifecycle.isDestroyed
    @JvmStatic
		val isForeground
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
