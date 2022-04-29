package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import kotlinx.coroutines.*

class MediaServiceNotification(private val service: MediaService) : MediaNotification.Provider {

  val appScope = service.appScope
  val coiLHelper = service.coilHelper
  val dispatchers = service.dispatchers
  val itemHelper = service.itemHelper
  val protoRepo = service.protoRepo

  val mediaSession
    get() = service.mediaSession

  val manager = service
    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  val notificationBuilder = NotificationBuilder()

  init {
    if (VersionHelper.hasOreo()) createNotificationChannel()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    manager.createNotificationChannel(
      NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_NAME,
        NotificationManager.IMPORTANCE_LOW
      )
    )
  }

  override fun createNotification(
    mediaController: MediaController,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: MediaNotification.Provider.Callback
  ): MediaNotification {
    return MediaNotification(NOTIFICATION_ID, getNotification(mediaController, null))
      .also { launchNotificationCallback(mediaController, onNotificationChangedCallback) }
  }

  override fun handleCustomAction(
    mediaController: MediaController,
    action: String,
    extras: Bundle
  ) {}

  private var itemBitmap: Pair<MediaItem, Bitmap?> = Pair(MediaItem.EMPTY, null)

  private fun getNotification(
    controller: MediaController,
    icon: Bitmap? = null
  ): Notification {

    val showPlayButton = if (controller.playbackState.isStateBuffering()) {
      !controller.playWhenReady
    } else {
      !controller.isPlaying
    }

    val item = controller.currentMediaItem ?: MediaItem.EMPTY

    val largeIcon = icon ?: if (itemBitmap.first == item) itemBitmap.second else icon

    return notificationBuilder
      .buildMediaNotification(
        session = mediaSession,
        largeIcon = largeIcon,
        channelId = NOTIFICATION_CHANNEL_ID,
        currentItemIndex = controller.currentMediaItemIndex,
        currentItem = item,
        playerState = controller.playbackState,
        repeatMode = controller.repeatMode,
        showPrevButton = controller.hasPreviousMediaItem(),
        showNextButton = controller.hasNextMediaItem(),
        showPlayButton = showPlayButton,
        subtitle = item.mediaMetadata.artist.toString(),
        title = item.mediaMetadata.displayTitle.toString()
      )
  }

  private var notificationCallbackJob = Job().job
  private fun launchNotificationCallback(
    controller: MediaController,
    callback: MediaNotification.Provider.Callback
  ) {
    notificationCallbackJob.cancel()
    notificationCallbackJob = service.mainScope.launch {

      val item = controller.currentMediaItem ?: MediaItem.EMPTY

      val bitmap =
        withContext(dispatchers.io) { getSquaredBitmapFromItem(item) }

      val notification =
        MediaNotification(NOTIFICATION_ID, getNotification(controller, bitmap))

      ensureActive()
      itemBitmap = Pair(item, bitmap)
      callback.onNotificationChanged(notification)
      delay(500)
      ensureActive()
      callback.onNotificationChanged(notification)
    }
  }

  suspend fun getSquaredBitmapFromItem(item: MediaItem?): Bitmap? {
    var toReturn: Bitmap? = null
    if (item != null) {
      service.itemHelper.getEmbeddedPicture(item)
        ?.let { array ->
          toReturn = coiLHelper.squareBitmap(
            bitmap = BitmapFactory.decodeByteArray(array, 0, array.size),
            type = CoilHelper.CenterCropTransform.CENTER,
            size = 512,
          )
        }
    }
    return toReturn
  }

  inner class NotificationBuilder() {

    fun buildMediaNotification(
      session: MediaSession,
      largeIcon: Bitmap?,
      channelId: String,
      currentItemIndex: Int,
      currentItem: MediaItem,
      @Player.State playerState: Int,
      @Player.RepeatMode repeatMode: Int,
      showPrevButton: Boolean,
      showNextButton: Boolean,
      showPlayButton: Boolean,
      subtitle: String,
      title: String,
    ): Notification {

      return NotificationCompat
        .Builder(service, channelId).apply {

          val contentIntent = PendingIntent.getActivity(
            service, 445,
            service.packageManager.getLaunchIntentForPackage(service.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )

          val ongoing = playerState.isStateBuffering() && playerState.isStateReady()
          val style = MediaStyleNotificationHelper.DecoratedMediaCustomViewStyle(session)
          style.setShowActionsInCompactView(1, 2, 3)

          setSmallIcon(R.drawable.play_icon_theme3)
          setContentIntent(contentIntent)
          setContentTitle(title)
          setContentText(subtitle)
          setChannelId(channelId)
          setOngoing(ongoing)

          when (repeatMode) {
            Player.REPEAT_MODE_OFF -> addAction(actionRepeatOff)
            Player.REPEAT_MODE_ONE -> addAction(actionRepeatOne)
            Player.REPEAT_MODE_ALL -> addAction(actionRepeatAll)
          }

          if (showPrevButton) addAction(actionPrev) else addAction(actionPrevDisabled)
          if (showPlayButton) addAction(actionPlay) else addAction(actionPause)
          if (showNextButton) addAction(actionNext) else addAction(actionNextDisabled)
          addAction(actionStopCancel)

          setLargeIcon(largeIcon)
          setStyle(style)
        }
        .build()
    }

    private val actionStopCancel = makeActionStopCancel()
    private val actionPlay = makeActionPlay()
    private val actionPause = makeActionPause()
    private val actionPrev = makeActionPrev()
    private val actionPrevDisabled = makeActionPrevDisabled()
    private val actionNext = makeActionNext()
    private val actionNextDisabled = makeActionNextDisabled()
    private val actionRepeatOff = makeActionRepeatOffToOne()
    private val actionRepeatOne = makeActionRepeatOneToAll()
    private val actionRepeatAll = makeActionRepeatAllToOff()

    private fun makeActionStopCancel(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_close
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_STOP_CANCEL)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_STOP_CANCEL_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatOneToAll(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_one
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ONE_TO_ALL)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_ONE_TO_ALL_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatOffToOne(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_off
      val title = "ACTION_REPEAT_OFF_TO_ONE"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_OFF_TO_ONE)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_OFF_TO_ONE_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionRepeatAllToOff(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_repeat_all
      val title = "ACTION_REPEAT_ALL_TO_OFF"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ALL_TO_OFF)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_REPEAT_ALL_TO_OFF_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPlay(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_play
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PLAY)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PLAY_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPause(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_pause
      val title = "ACTION_PAUSE"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PAUSE)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PAUSE_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionNext(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_next
      val title = "ACTION_NEXT"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_NEXT_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionNextDisabled(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_next_disabled
      val title = "ACTION_NEXT_DISABLED"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT_DISABLED)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_NEXT_DISABLED_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPrev(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_prev
      val title = "ACTION_PREV"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PREV_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }

    private fun makeActionPrevDisabled(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_prev_disabled
      val title = "ACTION_PREV_DISABLED"
      val intent = Intent(PLAYBACK_CONTROL_INTENT).apply {
        putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV_DISABLED)
        setPackage(service.packageName)
      }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      val pIntent = PendingIntent.getBroadcast(service, ACTION_PREV_DISABLED_CODE, intent, flag)
      return NotificationCompat.Action.Builder(resId, title, pIntent).build()
    }
  }

  companion object {
    const val NOTIFICATION_CHANNEL_ID = "Media Service Channel"
    const val NOTIFICATION_NAME = "Media Service Notification"
    const val NOTIFICATION_ID = 301

    const val PLAYBACK_CONTROL_INTENT = "com.kylennt.mediaplayer.PLAYBACK_CONTROL_INTENT"
    const val PLAYBACK_CONTROL_ACTION = "PLAYBACK_CONTROL_ACTION"
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
  }
}
