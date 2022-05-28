package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.annotation.ThreadSafe
import com.kylentt.mediaplayer.core.delegates.LockMainThread
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkNotMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext

/**
 * [MediaNotification.Provider] Implementation for [MediaService]
 * must be Initialized after [MediaLibraryService] Super.onCreate()
 * @throws [IllegalStateException] if [MediaLibraryService] baseContext is null
 * @author Kylentt
 * @since 2022/04/30
 */

interface MediaServiceNotificationProvider : MediaNotification.Provider {
	val notificationManager: NotificationManager
	val mediaNotificationChannelName: String
	val mediaNotificationName: String
	val mediaNotificationId: Int

	@MainThread fun getSessionNotification(session: MediaSession): Notification
	@MainThread fun updateSessionNotification(session: MediaSession): Unit
	@MainThread fun considerForegroundService(player: Player, notification: Notification)
	@MainThread fun considerForegroundService(session: MediaSession)

	@ThreadSafe suspend fun suspendUpdateNotification(
		session: MediaSession
	)

	@ThreadSafe suspend fun suspendHandleMediaItemTransition(
		item: MediaItem,
		@Player.MediaItemTransitionReason reason: Int
	)

	@ThreadSafe suspend fun suspendHandleRepeatModeChanged(@Player.RepeatMode mode: Int)

	@ThreadSafe suspend fun dispatchSuspendNotificationValidator(times: Int, interval: Long, update: suspend () -> Unit)
}

class MediaServiceNotificationProviderImpl(
  private val service: MediaService
) : MediaServiceNotificationProvider {

	private val mediaService = service

  private val coiLHelper = service.coilHelper
  private val dispatchers = service.dispatchers
  private val itemHelper = service.itemHelper

  private val notificationBuilder = NotificationBuilder()
  private var notificationUpdateJob = Job().job

  private var itemBitmap = Pair<MediaItem, Bitmap?>(MediaItem.EMPTY, null)
	private var validatorJob by LockMainThread(Job().job)

	private val mediaSession
		get() = mediaService.currentMediaSession

	override val notificationManager
		get() = mediaService.notificationManager

	override val mediaNotificationChannelName: String
		get() = NOTIFICATION_CHANNEL_NAME

	override val mediaNotificationName: String
		get() = NOTIFICATION_NAME

	override val mediaNotificationId: Int
		get() = NOTIFICATION_ID

	init {
		checkNotNull(service.baseContext) {
			"Service Context must Not be null, try Initializing lazily after Super.onCreate()"
		}
		checkState(service.sessions.isEmpty()) {
			"This Provider must be initialized before MediaLibraryService.onGetSession()"
		}
		if (VersionHelper.hasOreo()) {
			createNotificationChannel(notificationManager)
		}
	}

	/**
	 * Create [NotificationChannel] for [VersionHelper.hasOreo]
	 * @param manager The Notification Manager
	 */

	@MainThread
	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannel(manager: NotificationManager) {
		val importance = NotificationManager.IMPORTANCE_LOW
		val channel = NotificationChannel(NOTIFICATION_CHANNEL_NAME, NOTIFICATION_NAME, importance)
		manager.createNotificationChannel(channel)
	}

	/**
	 * Update Notification Callback from [androidx.media3.session.MediaNotificationManager.MediaControllerListener]
	 * anyOf: [Player.EVENT_PLAYBACK_STATE_CHANGED], [Player.EVENT_PLAY_WHEN_READY_CHANGED], [Player.EVENT_MEDIA_METADATA_CHANGED]
	 * @param mediaController of the Linked [MediaSession]
	 * @param onNotificationChangedCallback Callback Method for Async Task
	 */

	@MainThread
	override fun createNotification(
		mediaController: MediaController,
		actionFactory: MediaNotification.ActionFactory,
		onNotificationChangedCallback: MediaNotification.Provider.Callback
	): MediaNotification {
		val notification = with(mediaSession) {
			getNotificationForSession(this, mediaNotificationChannelName)
		}
		service.mainScope.launch {
			suspendHandleMediaNotificationChangedCallback(onNotificationChangedCallback)
		}
		return MediaNotification(mediaNotificationId, notification)
	}

	override fun handleCustomAction(
		mediaController: MediaController,
		action: String,
		extras: Bundle
	) = Unit

	@MainThread
	override fun getSessionNotification(session: MediaSession): Notification {
		return getNotificationForSession(session, NOTIFICATION_CHANNEL_NAME)
	}

	@MainThread
	override fun updateSessionNotification(session: MediaSession) {
		service.notificationManager.notify(mediaNotificationId, getSessionNotification(session))
	}

	@MainThread
	private suspend fun suspendHandleMediaNotificationChangedCallback(
		callback: MediaNotification.Provider.Callback
	) {
		launchSessionNotificationValidator(mediaSession) {
			val mediaNotification = MediaNotification(mediaNotificationId, it)
			coroutineContext.ensureActive()
			callback.onNotificationChanged(mediaNotification)
		}
	}


	private suspend fun launchSessionNotificationValidator(
		session: MediaSession,
		onUpdate: suspend (Notification) -> Unit
	) = withContext(dispatchers.main) {
		dispatchSuspendNotificationValidator(2, 1000) {
			val player = session.player
			val notification =
				getNotificationForSession(session, mediaNotificationChannelName)
			onUpdate(notification)
			considerForegroundService(player, notification)
		}
	}

	override suspend fun suspendUpdateNotification(
		session: MediaSession
	): Unit = withContext(dispatchers.main) {
		val updateBlock: suspend () -> Unit = {
			val notification =
				getNotificationForSession(session, mediaNotificationChannelName)
			ensureActive()
			Timber.d("suspendUpdateNotification notify for ${session.player.currentMediaItem?.getDebugDescription()}")
			notificationManager.notify(mediaNotificationId, notification)
		}
		ensureActive()
		updateBlock()
		launchSessionNotificationValidator(session) { updateBlock() }
	}

	@MainThread
	override suspend fun dispatchSuspendNotificationValidator(
		times: Int,
		interval: Long,
		update: suspend () -> Unit
	) {
		checkMainThread()
		validatorJob.cancel()
		validatorJob = withContext(coroutineContext) {
			launch {
				repeat(times) {
					delay(interval)
					ensureActive()
					update()
				}
			}
		}
	}

	override suspend fun suspendHandleMediaItemTransition(
		item: MediaItem,
		reason: Int
	): Unit {
		withContext(dispatchers.io) {
			val bitmap = getMediaItemEmbeddedPicture(item)?.let { byteArray ->
				getBitmapFromByteArray(byteArray)
			}
			checkCancellation {
				bitmap?.recycle()
			}
			val squaredBitmap = bitmap?.let { resizeBitmapForNotification(it) }
			checkCancellation {
				bitmap?.recycle()
			}
			itemBitmap = item to squaredBitmap
		}

		withContext(dispatchers.main) {
			suspendUpdateNotification(mediaSession)
		}
	}

	override suspend fun suspendHandleRepeatModeChanged(mode: Int) {
		withContext(dispatchers.main) { suspendUpdateNotification(mediaSession) }
	}

	@WorkerThread
	private fun getMediaItemEmbeddedPicture(item: MediaItem): ByteArray? {
		checkNotMainThread()
		return itemHelper.getEmbeddedPicture(item)
	}

	@WorkerThread
	private fun getBitmapFromByteArray(bytes: ByteArray): Bitmap? {
		checkNotMainThread()
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
	}

	private inline fun CoroutineScope.checkCancellation(cleanUp: () -> Unit) {
		ifCancelled { cleanUp() }
		ensureActive()
	}

	private inline fun CoroutineScope.ifCancelled(block: () -> Unit) {
		if (!isActive) block()
	}

  /**
   * @param [player] the player
   * @return [itemBitmap]? if [player] MediaItem Bitmap is ready
   * @see itemBitmap
   */

	@MainThread
  private fun getItemBitmap(player: Player): Bitmap? {
		val equal = player.currentMediaItem.idEqual(itemBitmap.first)
		return elseNull(equal) { itemBitmap.second }
  }

	private inline fun <T> elseNull(condition: Boolean, block: () -> T): T? {
		return if (condition) block() else null
	}

  /**
   * @param player The [MediaSession]
   * @param icon The [Bitmap] for [Notification.Builder.setLargeIcon]
   * @param channelName The Channel Name this [Notification] belong to
   * @return [Notification] suitable for [MediaNotification]
   */

	@MainThread
  private fun getNotificationForSession(
		session: MediaSession,
		channelName: String
  ): Notification {
		val player = session.player
		val icon = getItemBitmap(player)

    val showPlayButton = if (player.playbackState.isStateBuffering()) {
      !player.playWhenReady
    } else {
      !player.isPlaying
    }

    val item = player.currentMediaItem ?: MediaItem.EMPTY

    Timber.d("getNotification for ${item.mediaMetadata.displayTitle}, " +
      "\nshowPlayButton: $showPlayButton"
    )

    return notificationBuilder
      .buildMediaNotification(
        session = mediaSession,
        largeIcon = icon,
        channelName = channelName,
        currentItemIndex = player.currentMediaItemIndex,
        currentItem = item,
        playerState = player.playbackState,
        repeatMode = player.repeatMode,
        showPrevButton = player.hasPreviousMediaItem(),
        showNextButton = player.hasNextMediaItem(),
        showPlayButton = showPlayButton,
        subtitle = item.mediaMetadata.artist.toString(),
        subtext = item.mediaMetadata.albumTitle.toString(),
        title = item.mediaMetadata.displayTitle.toString()
      )
  }

  @MainThread
 	override fun considerForegroundService(player: Player, notification: Notification) {
    checkMainThread()
    val isForeground = MediaService.isForeground
		when {
			player.playbackState.isOngoing() -> {
				if (!isForeground) {
					service.startServiceAsForeground(notification)
				} else {
					return
				}
			}
			!player.playbackState.isOngoing() -> {
				service.stopServiceAsForeground(false)
			}
		}
    Timber.d("considerForegroundService changed," +
      "\nonGoing: ${player.playbackState.isOngoing()}" +
      "\nwasForeground: $isForeground" +
      "\nisForeground: ${MediaService.isForeground}"
    )
  }

	@MainThread
	override fun considerForegroundService(session: MediaSession) {
		checkMainThread()
		val isForeground = MediaService.isForeground
		val player = session.player
		when {
			!player.playbackState.isOngoing() -> {
				service.stopServiceAsForeground(false)
			}
			player.playbackState.isOngoing() -> {
				if (!isForeground) {
					service.startServiceAsForeground(getSessionNotification(session))
				} else {
					return
				}
			}
		}
		Timber.d("considerForegroundService changed," +
			"\nonGoing: ${player.playbackState.isOngoing()}" +
			"\nwasForeground: $isForeground" +
			"\nisForeground: ${MediaService.isForeground}"
		)
	}

	@WorkerThread
	private suspend fun resizeBitmapForNotification(bitmap: Bitmap): Bitmap {
		val size = if (VersionHelper.hasR()) 256 else 512
		return coiLHelper.squareBitmap(bitmap = bitmap, size = size, fastPath = true)
	}

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return that != null && idEqual(that.mediaId)
  }

  private fun MediaItem?.idEqual(that: String): Boolean {
    return this != null && that.isNotBlank() && this.mediaId == that
  }

  inner class NotificationBuilder() {

    private val actionStopCancel by lazy { makeActionStopCancel(MediaItem.EMPTY) }
    private val actionPlay by lazy { makeActionPlay(MediaItem.EMPTY) }
    private val actionPause by lazy { makeActionPause(MediaItem.EMPTY) }
    private val actionPrev by lazy { makeActionPrev(MediaItem.EMPTY) }
    private val actionPrevDisabled by lazy { makeActionPrevDisabled(MediaItem.EMPTY) }
    private val actionNext by lazy { makeActionNext(MediaItem.EMPTY) }
    private val actionNextDisabled by lazy { makeActionNextDisabled(MediaItem.EMPTY) }
    private val actionRepeatOff by lazy { makeActionRepeatOffToOne(MediaItem.EMPTY) }
    private val actionRepeatOne by lazy { makeActionRepeatOneToAll(MediaItem.EMPTY) }
    private val actionRepeatAll by lazy { makeActionRepeatAllToOff(MediaItem.EMPTY) }

    private val intentDismissNotification by lazy { makeDismissPendingIntent() }

    fun buildMediaNotification(
			session: MediaSession,
			largeIcon: Bitmap?,
			channelName: String,
			currentItemIndex: Int,
			currentItem: MediaItem,
			@Player.State playerState: Int,
			@Player.RepeatMode repeatMode: Int,
			showPrevButton: Boolean,
			showNextButton: Boolean,
			showPlayButton: Boolean,
			subtitle: String,
			subtext: String,
			title: String,
    ): Notification {

      return NotificationCompat.Builder(service, channelName)
        .apply {

          val contentIntent = PendingIntent
            .getActivity(service, 445,
              service.packageManager.getLaunchIntentForPackage(service.packageName),
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

          val deleteIntent = intentDismissNotification

          val onGoing = playerState.isOngoing()

          val style = MediaStyleNotificationHelper
            .DecoratedMediaCustomViewStyle(session)
            .setShowActionsInCompactView(1,2,3)

          setColorized(true)
          setContentIntent(contentIntent)
          setContentTitle(title)
          setContentText(subtitle)
          setChannelId(channelName)
          setDeleteIntent(deleteIntent)
          setShowWhen(false)
          setSmallIcon(R.drawable.play_icon_theme3)

          if (subtext.isNotBlank()) setSubText(subtext)

          setOngoing(onGoing)

          val repeatAction = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> makeActionRepeatOffToOne(currentItem)
            Player.REPEAT_MODE_ONE -> makeActionRepeatOneToAll(currentItem)
            Player.REPEAT_MODE_ALL -> makeActionRepeatAllToOff(currentItem)
            else -> throw IllegalArgumentException("invalid repeat Mode, " +
              "\n current = $repeatMode" +
              "\n actual = ${session.player.repeatMode}" +
              "should never reach here"
            )
          }
          val prevAction = if (showPrevButton) {
            makeActionPrev(currentItem)
          } else {
            makeActionPrevDisabled(currentItem)
          }
          val playAction = if (showPlayButton) {
            makeActionPlay(currentItem)
          } else {
            makeActionPause(currentItem)
          }
          val nextAction = if (showNextButton) {
            makeActionNext(currentItem)
          } else {
            makeActionNextDisabled(currentItem)
          }

          addAction(repeatAction)
          addAction(prevAction)
          addAction(playAction)
          addAction(nextAction)
          addAction(makeActionStopCancel(currentItem))

          setLargeIcon(largeIcon)
          setStyle(style)
        }
        .build()
    }

    private fun makeDismissPendingIntent(): PendingIntent {
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_DISMISS_NOTIFICATION)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      return PendingIntent.getBroadcast(service, ACTION_DISMISS_NOTIFICATION_CODE, intent, flag)
    }

    private fun makeActionStopCancel(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_close
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_STOP_CANCEL)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_STOP_CANCEL_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatOneToAll(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_one
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ONE_TO_ALL)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_ONE_TO_ALL_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatOffToOne(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_off
      val title = "ACTION_REPEAT_OFF_TO_ONE"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_OFF_TO_ONE)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_OFF_TO_ONE_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatAllToOff(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_all
      val title = "ACTION_REPEAT_ALL_TO_OFF"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ALL_TO_OFF)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_ALL_TO_OFF_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPlay(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_play
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PLAY)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PLAY_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPause(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_pause
      val title = "ACTION_PAUSE"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PAUSE)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PAUSE_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionNext(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_next
      val title = "ACTION_NEXT"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_NEXT_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionNextDisabled(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_next_disabled
      val title = "ACTION_NEXT_DISABLED"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT_DISABLED)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_NEXT_DISABLED_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPrev(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_prev
      val title = "ACTION_PREV"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PREV_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPrevDisabled(item: MediaItem): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_prev_disabled
      val title = "ACTION_PREV_DISABLED"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV_DISABLED)
          putExtra(PLAYBACK_ITEM_ID, item.mediaId)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PREV_DISABLED_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }
  }

  companion object {
    const val NOTIFICATION_CHANNEL_NAME = "Media Service Channel"
    const val NOTIFICATION_NAME = "Media Service Notification"
    const val NOTIFICATION_ID = 301

    const val PLAYBACK_CONTROL_INTENT = "com.kylennt.mediaplayer.PLAYBACK_CONTROL_INTENT"
    const val PLAYBACK_CONTROL_ACTION = "PLAYBACK_CONTROL_ACTION"
    const val PLAYBACK_ITEM_ID = "PLAYBACK_ITEM_ID"
    const val ACTION_STOP_CANCEL = "ACTION_STOP_CANCEL"
    const val ACTION_NEXT = "ACTION_NEXT"
    const val ACTION_PREV = "ACTION_PREV"
    const val ACTION_NEXT_DISABLED = "ACTION_NEXT_DISABLED"
    const val ACTION_PREV_DISABLED = "ACTION_PREV_DISABLED"
    const val ACTION_PLAY = "ACTION_PLAY"
    const val ACTION_PAUSE = "ACTION_PAUSE"

    const val ACTION_REPEAT_OFF_TO_ONE = "ACTION_REPEAT_OFF_TO_ONE"
    const val ACTION_REPEAT_ONE_TO_ALL = "ACTION_REPEAT_ONE_TO_ALL"
    const val ACTION_REPEAT_ALL_TO_OFF = "ACTION_REPEAT_ALL_TO_OFF"

    const val ACTION_DISMISS_NOTIFICATION = "ACTION_DISMISS_NOTIFICATION"

    const val ACTION_STOP_CANCEL_CODE = 400
    const val ACTION_PLAY_CODE = 401
    const val ACTION_PAUSE_CODE = 402
    const val ACTION_NEXT_CODE = 403
    const val ACTION_PREV_CODE = 404
    const val ACTION_NEXT_DISABLED_CODE = 405
    const val ACTION_PREV_DISABLED_CODE = 406
    const val ACTION_REPEAT_OFF_TO_ONE_CODE = 407
    const val ACTION_REPEAT_ONE_TO_ALL_CODE = 408
    const val ACTION_REPEAT_ALL_TO_OFF_CODE = 409
    const val ACTION_DISMISS_NOTIFICATION_CODE = 410

    const val NOTIFICATION_UPDATE_INTERVAL = 500L
  }
}
