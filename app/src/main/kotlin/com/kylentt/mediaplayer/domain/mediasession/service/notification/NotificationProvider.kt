package com.kylentt.mediaplayer.domain.mediasession.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.media3.session.*
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.delegates.LockMainThread
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.mediaItem.MediaItemHelper.getDebugDescription
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.ServiceLifecycleState
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand.Companion.wrapWithFadeOut
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkNotMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper.Companion.orEmpty
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext

/**
 * [MediaNotification.Provider] Implementation for [MusicLibraryService]
 * must be Initialized after [MediaLibraryService] Super.onCreate()
 * @throws [IllegalStateException] if [MediaLibraryService] baseContext is null
 * @author Kylentt
 * @since 2022/04/30
 */

class MusicLibraryNotificationProvider(
	private val service: MusicLibraryService
) : MediaNotification.Provider {

	private var itemBitmap = Pair<MediaItem, Bitmap?>(MediaItem.EMPTY, null)

	private val serviceState: ServiceLifecycleState by MusicLibraryService.LifecycleState
	private val dispatchers: AppDispatchers = service.coroutineDispatchers

  private val notificationBuilder = NotificationBuilder()

	private val mediaSession
		get() = service.currentMediaSession

	val notificationManager: NotificationManager by lazy {
		service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	val notificationActionReceiver = NotificationActionReceiver()

	val mediaNotificationChannelName: String
		get() = NOTIFICATION_CHANNEL_NAME

	val mediaNotificationName: String
		get() = NOTIFICATION_NAME

	val mediaNotificationId: Int
		get() = NOTIFICATION_ID

	init {
		checkNotNull(service.baseContext) {
			"Service Context must Not be null, try Initializing lazily at least onCreate()"
		}
		checkState(service.sessions.isEmpty()) {
			"This Provider must be initialized before MediaLibraryService.onGetSession()"
		}
		if (VersionHelper.hasOreo()) {
			createNotificationChannel(notificationManager)
		}
		service.registerReceiver(notificationActionReceiver, IntentFilter(PLAYBACK_CONTROL_INTENT))
		service.registerOnDestroyCallback { service.unregisterReceiver(notificationActionReceiver) }
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

		Timber.d("createNotification request for " +
			"${mediaController.currentMediaItem?.getDebugDescription()}"
		)

		val notification = getNotificationForSession(mediaSession, mediaNotificationChannelName)
		return MediaNotification(mediaNotificationId, notification)
	}

	@MainThread
	override fun handleCustomAction(
		mediaController: MediaController,
		action: String,
		extras: Bundle
	) = Unit

	@MainThread
	fun getSessionNotification(session: MediaSession): Notification {
		return getNotificationForSession(session, NOTIFICATION_CHANNEL_NAME)
	}

	@MainThread
	fun updateSessionNotification(session: MediaSession) {
		notificationManager.notify(mediaNotificationId, getSessionNotification(session))
	}

	@MainThread
	fun updateSessionNotification(notification: Notification) {
		notificationManager.notify(mediaNotificationId, notification)
	}

	private var validatorJob = Job().job
	suspend fun launchSessionNotificationValidator(
		session: MediaSession,
		onUpdate: suspend (Notification) -> Unit
	) = withContext(dispatchers.main) {
		validatorJob.cancel()
		validatorJob = launch {
			dispatchSuspendNotificationValidator(2, NOTIFICATION_UPDATE_INTERVAL) {
				val notification = getNotificationForSession(session, mediaNotificationChannelName)
				considerForegroundService(session.player, notification)
				onUpdate(notification)
			}
		}
	}

	private suspend fun suspendUpdateNotification(
		session: MediaSession
	): Unit = withContext(dispatchers.main) {
		updateSessionNotification(session)
		launchSessionNotificationValidator(session) {
			ensureActive()
			updateSessionNotification(it)
		}
	}

	@MainThread
	private suspend fun dispatchSuspendNotificationValidator(
		times: Int,
		interval: Long,
		update: suspend () -> Unit
	) = withContext(dispatchers.main) {
		repeat(times) {
			delay(interval)
			ensureActive()
			update()
		}
	}

	suspend fun suspendHandleMediaItemTransition(
		item: MediaItem,
		reason: Int // TODO
	) = withContext(dispatchers.main) {
		ensureActive()

		withContext(dispatchers.io) {
			val bitmap = getMediaItemEmbeddedPicture(item)?.let { byteArray ->
				getBitmapFromByteArray(byteArray)
			}

			checkCancellation { bitmap?.recycle() }
			val squaredBitmap = bitmap?.let { resizeBitmapForNotification(it) }

			checkCancellation {
				// Coil might give the same instance so squaredBitmap is not recycled
				bitmap?.recycle()
			}
			itemBitmap = item to squaredBitmap
		}

		suspendUpdateNotification(mediaSession)
	}

	suspend fun suspendHandleRepeatModeChanged(mode: Int) {
		withContext(dispatchers.main) { suspendUpdateNotification(mediaSession) }
	}

	@WorkerThread
	private fun getMediaItemEmbeddedPicture(item: MediaItem): ByteArray? {
		checkNotMainThread()
		return service.mediaItemHelper.getEmbeddedPicture(item)
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
	 * @param session The [MediaSession]
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

		val item = player.currentMediaItem.orEmpty()

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
 	fun considerForegroundService(player: Player, notification: Notification) {
    checkMainThread()
		when {
			player.playbackState.isOngoing() -> {
				if (serviceState.isForeground()) return
				service.startForegroundService(mediaNotificationId, notification)
			}
			!player.playbackState.isOngoing() -> {
				service.stopForegroundService(mediaNotificationId, false)
			}
		}
    Timber.d("considerForegroundService changed," +
      "\nonGoing: ${player.playbackState.isOngoing()}" +
      "\nisForeground: ${MusicLibraryService.LifecycleState.isForeground()}"
    )
  }

	@MainThread
	fun considerForegroundService(session: MediaSession) {
		checkMainThread()
		val player = session.player
		when {
			player.playbackState.isOngoing() -> {
				if (serviceState.isForeground()) return
				service.startForegroundService(mediaNotificationId, getSessionNotification(session))
			}
			!player.playbackState.isOngoing() -> {
				service.stopForegroundService(mediaNotificationId, false)
			}
		}
		Timber.d("considerForegroundService changed," +
			"\nonGoing: ${player.playbackState.isOngoing()}" +
			"\nisForeground: ${MusicLibraryService.LifecycleState.isForeground()}"
		)
	}

	inner class NotificationActionReceiver : BroadcastReceiver() {

		override fun onReceive(p0: Context?, p1: Intent?) {
			p1?.let { handleNotificationActionImpl(it) }
		}

		@MainThread
		fun handleNotificationActionImpl(intent: Intent) {
			val action = intent.getStringExtra(PLAYBACK_CONTROL_ACTION) ?: return
			if (mediaSession.player.currentMediaItem == null) {
				return validateItem(intent)
			}
			when (action) {
				ACTION_PLAY -> onReceiveActionPlay()
				ACTION_PAUSE -> onReceiveActionPause()
				ACTION_NEXT -> onReceiveActionNext()
				ACTION_PREV -> onReceiveActionPrev()
				ACTION_REPEAT_OFF_TO_ONE -> onReceiveRepeatOffToOne()
				ACTION_REPEAT_ONE_TO_ALL -> onReceiveRepeatOneToAll()
				ACTION_REPEAT_ALL_TO_OFF -> onReceiveRepeatAllToOff()
				ACTION_STOP_CANCEL -> onReceiveStopCancel()
				ACTION_DISMISS_NOTIFICATION -> service.stopForegroundService(mediaNotificationId, true)
			}
		}

		private var isValidating = false
		private fun validateItem(intent: Intent) = with(mediaSession.player) {
			Timber.d("checkItem CurrentMediaItem: $currentMediaItem")

			if (currentMediaItem == null && !isValidating) {
				isValidating = true

				val mediaId = intent.getStringExtra(PLAYBACK_ITEM_ID)
				when {
					mediaId == null -> Unit
					mediaId.startsWith(MediaStoreSong.MEDIA_ID_PREFIX) -> {
						service.mainScope
							.launch {
								val item = service.mediaRepository.getMediaStoreSongById(mediaId)
								item?.let { setMediaItem(it.asMediaItem) }
								isValidating = false
							}
					}
				}
			}
		}

		private fun onReceiveActionPlay(): Unit = with(mediaSession.player) {
			if (playWhenReady && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}

			when {
				playbackState.isStateEnded() -> {
					if (hasNextMediaItem()) seekToNextMediaItem()
					seekTo(0L)
				}
				playbackState.isStateIdle() -> {
					prepare()
				}
			}

			playWhenReady = true
		}

		private fun onReceiveActionPause() = with(mediaSession.player) {
			if (!playWhenReady && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(playbackState.isOngoing())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Pause received when $this is Idle / Ended")
			} finally {
				playWhenReady = false
			}
		}

		private fun onReceiveActionNext() = with(mediaSession.player) {
			if (!hasNextMediaItem() && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(hasNextMediaItem())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Next received when $this has no Next Item")
			} finally {
				seekToNextMediaItem()
			}
		}

		private fun onReceiveActionPrev() = with(mediaSession.player) {
			if (!hasPreviousMediaItem() && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(hasPreviousMediaItem())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Previous received when $this has no Previous Item")
			} finally {
				seekToPreviousMediaItem()
			}
		}

		private fun onReceiveRepeatOffToOne() = with(mediaSession.player) {
			if (!repeatMode.isRepeatOff() && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(repeatMode.isRepeatOff())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOne received when $this Repeat is not Off")
			} finally {
				repeatMode = Player.REPEAT_MODE_ONE
			}
		}

		private fun onReceiveRepeatOneToAll() = with(mediaSession.player) {
			if (!repeatMode.isRepeatOne() && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(repeatMode.isRepeatOne())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToAll received when $this Repeat is not One")
			} finally {
				repeatMode = Player.REPEAT_MODE_ALL
			}
		}

		private fun onReceiveRepeatAllToOff() = with(mediaSession.player) {
			if (!repeatMode.isRepeatAll() && playbackState.isOngoing()) {
				return updateSessionNotification(mediaSession)
			}
			try {
				checkState(repeatMode.isRepeatAll())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOff received when $this Repeat is not All")
			} finally {
				repeatMode = Player.REPEAT_MODE_OFF
			}
		}

		private fun onReceiveStopCancel() = with(mediaSession.player) {
			val duration = 1000L
			val removeNotification = !playbackState.isOngoing()
			val command = ControllerCommand.STOP.wrapWithFadeOut(flush = true, duration = duration) {
				val foreground = serviceState.isForeground()
				service.stopForegroundService(mediaNotificationId, removeNotification)
				if (!foreground && !com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.isAlive) {
					// NPE in MediaControllerImplBase.java:3041 when calling librarySession.release()
					service.onDestroy()
				}
			}
			service.sessionConnector.sendControllerCommand(command)
		}
	}

	@WorkerThread
	private suspend fun resizeBitmapForNotification(bitmap: Bitmap): Bitmap {
		val size = if (VersionHelper.hasR()) 256 else 512
		return service.coilHelper.squareBitmap(bitmap = bitmap, size = size, fastPath = true)
	}

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return idEqual(that?.mediaId.toString())
  }

  private fun MediaItem?.idEqual(that: String): Boolean {
    return this?.mediaId == that
  }

  inner class NotificationBuilder() {

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

          val deleteIntent = makeDismissPendingIntent(currentItem)

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

    private fun makeDismissPendingIntent(item: MediaItem): PendingIntent {
      val intent = Intent(PLAYBACK_CONTROL_INTENT)
        .apply {
          putExtra(PLAYBACK_CONTROL_ACTION, ACTION_DISMISS_NOTIFICATION)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
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

    const val NOTIFICATION_UPDATE_INTERVAL = 1000L
  }
}
