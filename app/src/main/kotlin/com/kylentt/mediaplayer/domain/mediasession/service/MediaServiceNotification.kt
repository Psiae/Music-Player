package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.app.delegates.image.RecyclePairBitmap
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkNotMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext


/**
 * [MediaNotification.Provider] Implementation for [MediaService]
 * must be Initialized after [Service] Super.onCreate()
 * @throws [IllegalStateException] if [Service] baseContext is null
 * @author Kylentt
 * @since 2022/04/30
 */

class MediaServiceNotification(
  private val service: MediaService
) : MediaNotification.Provider {

  private val appScope = service.appScope
  private val coiLHelper = service.coilHelper
  private val dispatchers = service.dispatchers
  private val itemHelper = service.itemHelper
  private val sessionManager = service.sessionManager
  private val protoRepo = service.protoRepo

  private val mediaSession
    get() = service.currentMediaSession

  private val notificationBuilder: NotificationBuilder

  val notificationManager: NotificationManager

  private var notificationUpdateJob = Job().job

  private var itemBitmap by RecyclePairBitmap(MediaItem.EMPTY to null)

  private val playerListener = object : Player.Listener {

    private var itemTransitionJob = Job().job

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
      super.onMediaItemTransition(mediaItem, reason)
      val item = mediaItem ?: MediaItem.EMPTY
      launchItemTransitionJob(item)
    }

    private fun launchItemTransitionJob(item: MediaItem) {
      itemTransitionJob.cancel()
      itemTransitionJob = service.ioScope.launch {
        val bitmap = itemHelper.getEmbeddedPicture(item)?.let { byteArray ->
          resizeBitmapForNotification(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
        }
        ensureActive()
        itemBitmap = item to bitmap
        withContext(dispatchers.main) { suspendingUpdateNotification(mediaSession, false) }
      }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
      super.onRepeatModeChanged(repeatMode)
      updateNotification(mediaSession)
    }
  }

  init {
    checkNotNull(service.baseContext) {
      "Service Context must Not be null, try Initializing lazily after Super.onCreate()"
    }
    checkState(service.sessions.isEmpty()) {
      "This Provider should be called before MediaLibraryService.onGetSession()"
    }

    notificationBuilder = NotificationBuilder()

    notificationManager =
      service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (VersionHelper.hasOreo()) {
      createNotificationChannel(notificationManager)
    }

    addPlayerListener(playerListener)
  }

  /**
   * Create [NotificationChannel] for [VersionHelper.hasOreo]
   * @param manager The Notification Manager
   */

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel(manager: NotificationManager) {
    val importance = NotificationManager.IMPORTANCE_LOW
    val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, importance)
    manager.createNotificationChannel(channel)
  }

  /**
   * add [Player.Listener] to [MediaService.sessionPlayer] to update Notification,
   * might change this in the future for possible [Player] change in [MediaSession]
   * @param listener the Listener Object
   * @see removePlayerListener
   */

  private fun addPlayerListener(listener: Player.Listener) {
    service.whenSessionReady {
      val player = it.sessionPlayer
      it.registerListener(player, listener)
      service.onDestroyCallback.add {
        Timber.d("addPlayerListener onDestroyCallback, removePlayerListener")
        removePlayerListener(listener)
      }
    }
  }

  /**
   * remove [Player.Listener] from [MediaService.serviceSession] before releasing,
   * might change in the future for possible [Player] change in [MediaSession]
   * @param listener the Listener Object
   * @see addPlayerListener
   */


  private fun removePlayerListener(listener: Player.Listener) {
    service.whenSessionReady {
      Timber.d("addPlayerListener onDestroyCallback, unregisteringPlayerListener")
      it.unregisterListener(listener)
    }
  }

  /**
   * @param [player] the player
   * @return [itemBitmap]? if [player] MediaItem Bitmap is ready
   * @see itemBitmap
   * @see createNotification
   * @see updateNotification
   */

  private fun getItemBitmap(player: Player): Bitmap? {
    return if (player.currentMediaItem.idEqual(itemBitmap.first)) {
      itemBitmap.second
    } else {
      null
    }
  }

  /**
   * function to update [Notification] with [notificationManager] when [MediaNotification.Provider.Callback]
   * is not available, e.g: [Player.Listener.onRepeatModeChanged]
   * @see [suspendingUpdateNotification]
   */

  @MainThread
  fun updateNotification(session: MediaSession) {
    checkMainThread()
    val channelId = NOTIFICATION_CHANNEL_ID
    val notification =
      getNotificationFromPlayer(session.player, getItemBitmap(session.player), channelId)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  @MainThread
  suspend fun suspendingUpdateNotification(session: MediaSession, cancelCurrent: Boolean) =
    withContext(coroutineContext) {
      checkMainThread()
      val update = {
        val channelId = NOTIFICATION_CHANNEL_ID
        val notification =
          getNotificationFromPlayer(session.player, getItemBitmap(session.player), channelId)
        ensureActive()
        notificationManager.notify(NOTIFICATION_ID, notification)
      }
      if (isActive) {
        if (!cancelCurrent) return@withContext update()
        notificationUpdateJob.cancel()
        notificationUpdateJob = launch { update() }
      }
    }
  /**
   * Update Notification Callback from [androidx.media3.session.MediaNotificationManager.MediaControllerListener]
   * anyOf: [Player.EVENT_PLAYBACK_STATE_CHANGED], [Player.EVENT_PLAY_WHEN_READY_CHANGED], [Player.EVENT_MEDIA_METADATA_CHANGED]
   * @param mediaController of the Linked [MediaSession]
   * @param onNotificationChangedCallback Callback Method for Async Task
   * @see launchNotificationCallback
   */

  override fun createNotification(
    mediaController: MediaController,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: MediaNotification.Provider.Callback
  ): MediaNotification {
    getItemBitmap(mediaController).let {
      val channelId = NOTIFICATION_CHANNEL_ID
      launchNotificationCallback(mediaController, onNotificationChangedCallback)
      return MediaNotification(NOTIFICATION_ID, getNotificationFromPlayer(mediaController, it, channelId))
    }
  }

  override fun handleCustomAction(
    mediaController: MediaController,
    action: String,
    extras: Bundle
  ) = Unit

  /**
   * @param player The [Player]
   * @param icon The [Bitmap] for [Notification.Builder.setLargeIcon]
   * @param channelId The CHANNEL_ID this [Notification] belong to
   * @return [Notification] suitable for [MediaNotification]
   */

  private fun getNotificationFromPlayer(
    player: Player,
    icon: Bitmap? = null,
    channelId: String
  ): Notification {

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
        channelId = channelId,
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

  /**
   * Job to make sure current [MediaNotification] is up-to-date and handle drop by [notificationManager]
   * @param player The [Player]
   * @param callback The [MediaNotification.Provider.Callback] to send the [MediaNotification]
   */

  private fun launchNotificationCallback(
    player: Player,
    callback: MediaNotification.Provider.Callback
  ) {
    notificationUpdateJob.cancel()
    notificationUpdateJob = service.mainScope.launch {
      dispatchMediaNotificationValidator(NOTIFICATION_UPDATE_INTERVAL, player) {
        callback.onNotificationChanged(it)
      }
    }
  }

  private suspend fun dispatchMediaNotificationValidator(
    eachDelay: Long,
    player: Player,
    onUpdate: (MediaNotification) -> Unit
  ) = withContext(coroutineContext) {
    checkMainThread()
    repeat(2) {
      delay(eachDelay)

      ensureActive()
      val channelId = NOTIFICATION_CHANNEL_ID
      val mediaNotification =
        MediaNotification(NOTIFICATION_ID, getNotificationFromPlayer(player, getItemBitmap(player), channelId))

      ensureActive()
      onUpdate(mediaNotification)
      considerForegroundService(player, mediaNotification.notification)
    }
  }

  @MainThread
  private fun considerForegroundService(player: Player, notification: Notification) {
    checkMainThread()
    val isForeground = MediaService.isForeground
    if (isForeground && player.playbackState.isOngoing()) return
    if (player.playbackState.isOngoing()) {
      service.startServiceAsForeground(NOTIFICATION_ID, notification)
    }
    if (!player.playbackState.isOngoing()) {
      service.stopServiceFromForeground(false)
    }
    Timber.d("considerForegroundService changed," +
      "\nonGoing: ${player.playbackState.isOngoing()}" +
      "\nwasForeground: $isForeground" +
      "\nisForeground: ${MediaService.isForeground}"
    )
  }

  @WorkerThread
  private suspend fun resizeBitmapForNotification(bitmap: Bitmap): Bitmap =
    withContext(coroutineContext) {
      checkNotMainThread()
      val size = if (VersionHelper.hasR()) 256 else 512
      val resized = coiLHelper.squareBitmap(bitmap = bitmap, size = size, recycle = true)
      ensureActive()
      resized
    }

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return that != null && idEqual(that.mediaId)
  }

  private fun MediaItem?.idEqual(that: String): Boolean {
    return this != null && that.isNotBlank() && this.mediaId == that
  }

  inner class NotificationBuilder() {

    private val actionStopCancel by lazy { makeActionStopCancel() }
    private val actionPlay by lazy { makeActionPlay() }
    private val actionPause by lazy { makeActionPause() }
    private val actionPrev by lazy { makeActionPrev() }
    private val actionPrevDisabled by lazy { makeActionPrevDisabled() }
    private val actionNext by lazy { makeActionNext() }
    private val actionNextDisabled by lazy { makeActionNextDisabled() }
    private val actionRepeatOff by lazy { makeActionRepeatOffToOne() }
    private val actionRepeatOne by lazy { makeActionRepeatOneToAll() }
    private val actionRepeatAll by lazy { makeActionRepeatAllToOff() }

    private val intentDismissNotification by lazy { makeDismissPendingIntent() }

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
      subtext: String,
      title: String,
    ): Notification {

      return NotificationCompat.Builder(service, channelId)
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
          setChannelId(channelId)
          setDeleteIntent(deleteIntent)
          setShowWhen(false)
          setSmallIcon(R.drawable.play_icon_theme3)

          if (subtext.isNotBlank()) setSubText(subtext)

          setOngoing(onGoing)

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

    private fun makeDismissPendingIntent(): PendingIntent {
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_DISMISS_NOTIFICATION)
          setPackage(service.packageName)
        }
      val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      return PendingIntent.getBroadcast(service, ACTION_DISMISS_NOTIFICATION_CODE, intent, flag)
    }

    private fun makeActionStopCancel(): NotificationCompat.Action {
      val resId = R.drawable.ic_notif_close
      val title = "ACTION_PLAY"
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
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

    const val NOTIFICATION_UPDATE_INTERVAL = 1000L
  }
}
