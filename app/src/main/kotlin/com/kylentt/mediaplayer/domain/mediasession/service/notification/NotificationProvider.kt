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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.*
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.delegates.AutoCancelJob
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.media3.MediaItemFactory
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.domain.mediasession.service.LifecycleService
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.OnChanged
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand.Companion.wrapWithFadeOut
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkNotMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.core.media3.MediaItemFactory.orEmpty
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * [MediaNotification.Provider] Implementation for [MusicLibraryService]
 * must be Initialized after [MediaLibraryService] Super.onCreate()
 * @throws [IllegalStateException] if [MediaLibraryService] baseContext is null
 * @author Kylentt
 * @since 2022/04/30
 */

class MusicLibraryNotificationProvider(
	private val musicLibrary: MusicLibraryService,
	private val sessionManager: MusicLibrarySessionManager
) : MediaNotification.Provider {

	private val notificationBuilder = NotificationBuilder()
	private val notificationActionReceiver = NotificationActionReceiver()

	private val playerListenerImpl = PlayerListenerImpl()
	private val playerChangedImpl = PlayerChangedImpl()

	private val internalJob = SupervisorJob(musicLibrary.serviceJob)

	private val immediateScope = CoroutineScope(dispatchers.mainImmediate + internalJob)
	private val mainScope = CoroutineScope(dispatchers.main + internalJob)
	private val ioScope = CoroutineScope(dispatchers.io + internalJob)

	private var isInitialized: Boolean = false
	private var isReleased: Boolean = false
	private var itemBitmap = Pair<MediaItem, Bitmap?>(MediaItem.EMPTY, null)

	private val dispatchers: AppDispatchers
		get() = musicLibrary.coroutineDispatchers

	private val currentMediaSession
		get() = sessionManager.getCurrentMediaSession()

	private val notificationManager
		get() = musicLibrary.notificationManager

	val mediaNotificationChannelName: String
		get() = NOTIFICATION_CHANNEL_NAME

	val mediaNotificationName: String
		get() = NOTIFICATION_NAME

	val mediaNotificationId: Int
		get() = NOTIFICATION_ID

	init {
		checkState(musicLibrary.sessions.isEmpty()) {
			"This Provider must be initialized before MediaLibraryService.onGetSession()"
		}

		if (musicLibrary.baseContext != null) {
			initializeProvider()
		} else {

			val owner = musicLibrary as LifecycleService

			val obs = object : androidx.lifecycle.LifecycleEventObserver {

				override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
					if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
						initializeProvider()
						owner.lifecycle.removeObserver(this)
					}
				}

			}

			owner.lifecycle.addObserver(obs)
		}
	}

	@MainThread
	private fun initializeProvider() {
		checkMainThread()

		if (isInitialized) {
			return Timber.w("NotificationProvider initializeProvider() called twice")
		}

		checkState(musicLibrary.baseContext != null) {
			"Cannot Initialize NotificationProvider without Context, Initialize it in or after onCreate()"
		}

		if (VersionHelper.hasOreo()) {
			createNotificationChannel(notificationManager)
		}

		sessionManager.registerPlayerEventListener(playerListenerImpl)

		musicLibrary.registerReceiver(notificationActionReceiver, IntentFilter(PLAYBACK_CONTROL_INTENT))
		sessionManager.registerPlayerChangedListener(playerChangedImpl)

		isInitialized = true
	}

	/**
	 * Create [NotificationChannel] for [VersionHelper.hasOreo]
	 * @param manager The Notification Manager
	 */

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

	override fun createNotification(
		mediaController: MediaController,
		actionFactory: MediaNotification.ActionFactory,
		onNotificationChangedCallback: MediaNotification.Provider.Callback
	): MediaNotification {
		return createNotificationInternalImpl(
			mediaController, actionFactory, onNotificationChangedCallback
		)
	}

	override fun handleCustomAction(
		mediaController: MediaController,
		action: String,
		extras: Bundle
	) = Unit

	fun getSessionNotification(session: MediaSession): Notification {
		return getNotificationForSession(session, NOTIFICATION_CHANNEL_NAME)
	}

	fun updateNotificationFromSession(session: MediaSession) {
		notificationManager.notify(mediaNotificationId, getSessionNotification(session))
	}

	fun updateSessionNotification(notification: Notification) {
		if (sessionManager.getCurrentMediaSession() != null) {
			notificationManager.notify(mediaNotificationId, notification)
		}
	}

	private var validatorJob by AutoCancelJob()
	suspend fun launchSessionNotificationValidator(
		session: MediaSession,
		interval: Long = NOTIFICATION_UPDATE_INTERVAL,
		onUpdate: suspend (Notification) -> Unit
	) = withContext(dispatchers.main) {
		validatorJob = launch {
			dispatchSuspendNotificationValidator(2, interval) {
				val notification = getNotificationForSession(session, mediaNotificationChannelName)
				considerForegroundService(session.player, notification, true)
				onUpdate(notification)
			}
		}
	}

	private suspend fun suspendUpdateNotification(
		session: MediaSession
	): Unit = withContext(dispatchers.mainImmediate) {
		updateNotificationFromSession(session)
		launchSessionNotificationValidator(session) {
			ensureActive()
			updateSessionNotification(it)
		}
	}

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
		session: MediaSession,
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

		suspendUpdateNotification(session)
	}

	@WorkerThread
	private fun getMediaItemEmbeddedPicture(item: MediaItem): ByteArray? {
		checkNotMainThread()
		return MediaItemFactory.getEmbeddedImage(musicLibrary, item)
	}

	@WorkerThread
	private fun getBitmapFromByteArray(bytes: ByteArray): Bitmap? {
		checkNotMainThread()
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
	}

	private inline fun CoroutineScope.checkCancellation(ifCancelled: () -> Unit) {
		ifNotActive { ifCancelled() }
		ensureActive()
	}

	private inline fun CoroutineScope.ifNotActive(block: () -> Unit) {
		if (!isActive) block()
	}

  /**
   * @param [player] the player
   * @return [itemBitmap]? if [player] MediaItem Bitmap is ready
   * @see itemBitmap
   */

  private fun getItemBitmap(player: Player): Bitmap? {
		val equal = player.currentMediaItem.idEqual(itemBitmap.first)
		return elseNull(equal) { itemBitmap.second }
  }

	private inline fun <T> elseNull(condition: Boolean, block: () -> T): T? {
		return if (condition) block() else null
	}

	private val internalForegroundCondition: (Player) -> Boolean = { it.playWhenReady }

	private fun createNotificationInternalImpl(
		controller: MediaController,
		actionFactory: MediaNotification.ActionFactory,
		callback: MediaNotification.Provider.Callback
	): MediaNotification {

		val notification = getNotificationFromPlayer(controller, mediaNotificationChannelName)

		val shouldForeground = internalForegroundCondition(controller)

		if (musicLibrary.isServiceForeground != shouldForeground) {
			musicLibrary.updateForegroundState(shouldForeground)
		}

		return MediaNotification(mediaNotificationId, notification)
	}

	private fun getNotificationFromPlayer(
		player: Player,
		channelName: String
	): Notification {

		Timber.d("getNotificationFromPlayer for " +
			"${player.currentMediaItem?.mediaMetadata?.title}"
		)

		val icon = getItemBitmap(player)

		return notificationBuilder.buildMediaStyleNotification(player, icon, channelName)
	}

	private fun getNotificationForSession(
		session: MediaSession,
		channelName: String
	): Notification {

		Timber.d("getNotificationForSession for " +
			"${session.player.currentMediaItem?.mediaMetadata?.title}"
		)

		val icon = getItemBitmap(session.player)
		return notificationBuilder.buildMediaStyleNotification(session, icon, channelName)
	}

	val foregroundServiceCondition: (Player) -> Boolean = { it.playbackState.isOngoing() }

 	fun considerForegroundService(player: Player, notification: Notification, isValidator: Boolean) {
		when {
			foregroundServiceCondition(player) -> {
				if (musicLibrary.isServiceForeground) return
				musicLibrary.startForegroundService(mediaNotificationId, notification, true)
			}
			else -> {
				if (!musicLibrary.isServiceForeground) return
				musicLibrary.stopForegroundService(mediaNotificationId,
					removeNotification = false,
					isEvent = true
				)
			}
		}
  }

	@MainThread
	fun release() {
		checkMainThread()

		if (isReleased) {
			return
		}

		musicLibrary.unregisterReceiver(notificationActionReceiver)
		Timber.d("NotificationProvider receiver UnRegistered")

		isReleased = true
	}

	private inner class NotificationActionReceiver : BroadcastReceiver() {

		override fun onReceive(p0: Context?, p1: Intent?) {
			p1?.let { handleNotificationActionImpl(it) }
		}

		private fun handleNotificationActionImpl(intent: Intent) {
			val get = currentMediaSession ?: return

			val action = intent.getStringExtra(PLAYBACK_CONTROL_ACTION) ?: return
			if (get.player.currentMediaItem == null) {
				return validateItem(get, intent)
			}
			when (action) {
				ACTION_PLAY -> onReceiveActionPlay(get)
				ACTION_PAUSE -> onReceiveActionPause(get)
				ACTION_NEXT -> onReceiveActionNext(get)
				ACTION_PREV -> onReceiveActionPrev(get)
				ACTION_REPEAT_OFF_TO_ONE -> onReceiveRepeatOffToOne(get)
				ACTION_REPEAT_ONE_TO_ALL -> onReceiveRepeatOneToAll(get)
				ACTION_REPEAT_ALL_TO_OFF -> onReceiveRepeatAllToOff(get)
				ACTION_STOP_CANCEL -> onReceiveStopCancel(get)
				ACTION_DISMISS_NOTIFICATION -> onReceiveStopCancel(get)
			}
		}

		private var isValidating = false
		private fun validateItem(session: MediaSession, intent: Intent) = with(session.player) {
			Timber.d("checkItem CurrentMediaItem: $currentMediaItem")

			if (currentMediaItem == null && !isValidating) {
				isValidating = true

				val mediaId = intent.getStringExtra(PLAYBACK_ITEM_ID)
				when {
					mediaId == null -> musicLibrary.stopService(true)
					mediaId.startsWith(MediaStoreSong.MEDIA_ID_PREFIX) -> {
						musicLibrary.mainScope
							.launch {
								val item = musicLibrary.mediaRepository.getMediaStoreSongById(mediaId)
								item?.let { setMediaItem(it.asMediaItem) }
								isValidating = false
							}
					}
				}
			}
		}

		private fun onReceiveActionPlay(session: MediaSession): Unit = with(session.player) {
			if (playWhenReady && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
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

		private fun onReceiveActionPause(session: MediaSession) = with(session.player) {
			if (!playWhenReady && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(playbackState.isOngoing())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Pause received when $this is Idle / Ended")
			} finally {
				playWhenReady = false
			}
		}

		private fun onReceiveActionNext(session: MediaSession) = with(session.player) {
			if (!hasNextMediaItem() && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(hasNextMediaItem())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Next received when $this has no Next Item")
			} finally {
				seekToNextMediaItem()
			}
		}

		private fun onReceiveActionPrev(session: MediaSession) = with(session.player) {
			if (!hasPreviousMediaItem() && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(hasPreviousMediaItem())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification Previous received when $this has no Previous Item")
			} finally {
				seekToPreviousMediaItem()
			}
		}

		private fun onReceiveRepeatOffToOne(session: MediaSession) = with(session.player) {
			if (!repeatMode.isRepeatOff() && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(repeatMode.isRepeatOff())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOne received when $this Repeat is not Off")
			} finally {
				repeatMode = Player.REPEAT_MODE_ONE
			}
		}

		private fun onReceiveRepeatOneToAll(session: MediaSession) = with(session.player) {
			if (!repeatMode.isRepeatOne() && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(repeatMode.isRepeatOne())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToAll received when $this Repeat is not One")
			} finally {
				repeatMode = Player.REPEAT_MODE_ALL
			}
		}

		private fun onReceiveRepeatAllToOff(session: MediaSession) = with(session.player) {
			if (!repeatMode.isRepeatAll() && playbackState.isOngoing()) {
				return updateNotificationFromSession(session)
			}
			try {
				checkState(repeatMode.isRepeatAll())
			} catch (e: IllegalStateException) {
				Timber.e("$e: Notification RepeatToOff received when $this Repeat is not All")
			} finally {
				repeatMode = Player.REPEAT_MODE_OFF
			}
		}

		private fun onReceiveStopCancel(session: MediaSession) = with(session.player) {
			when {
				playbackState.isOngoing() -> {
					val command = ControllerCommand.STOP.wrapWithFadeOut(true, 1000L)
					musicLibrary.sessionConnector.sendControllerCommand(command)
				}
				else -> {
					Timber.d("onReceiveStopCancel $this not OnGoing")

					musicLibrary.stopForegroundService(mediaNotificationId,
						removeNotification = true, isEvent = musicLibrary.isServiceForeground
					)

					if (!MainActivity.isAlive) {
						musicLibrary.stopService(true)
					}
				}
			}
		}
	}

	@WorkerThread
	private suspend fun resizeBitmapForNotification(bitmap: Bitmap): Bitmap {
		val size = if (VersionHelper.hasR()) 256 else 512
		return musicLibrary.coilHelper.squareBitmap(bitmap = bitmap, size = size, fastPath = true)
	}

  private fun MediaItem?.idEqual(that: MediaItem?): Boolean {
    return idEqual(that?.mediaId.toString())
  }

  private fun MediaItem?.idEqual(that: String): Boolean {
    return this?.mediaId == that
  }

	private inner class PlayerListenerImpl : Player.Listener {

		private var updateNotificationJob by AutoCancelJob()
		private var mediaItemTransitionJob by AutoCancelJob()

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)

			val item = mediaItem ?: MediaItemFactory.EMPTY

			sessionManager.getCurrentMediaSession()?.let {
				mediaItemTransitionJob = mainScope.launch {
					suspendHandleMediaItemTransition(it, item, reason)
				}
			}
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)

			sessionManager.getCurrentMediaSession()?.let {
				updateNotificationJob = mainScope.launch { suspendUpdateNotification(it) }
			}
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			super.onPlaybackStateChanged(playbackState)

			sessionManager.getCurrentMediaSession()?.let { dispatchNotificationValidator(it) }
		}

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			super.onPlayWhenReadyChanged(playWhenReady, reason)

			sessionManager.getCurrentMediaSession()?.let { dispatchNotificationValidator(it) }
		}

		private fun dispatchNotificationValidator(session: MediaSession) {
			mainScope.launch {
				this@MusicLibraryNotificationProvider.launchSessionNotificationValidator(session) {
					updateSessionNotification(it)
				}
			}
		}
	}

	private inner class PlayerChangedImpl : OnChanged<Player> {

		override fun onChanged(old: Player?, new: Player) {
			checkArgument(old !== new)

			// null means player is first time changed or released
			// in both cases listener is empty
			/*old?.removeListener(playerListenerImpl)

			new.addListener(playerListenerImpl)*/
		}
	}

	private inner class NotificationBuilder {

		fun buildMediaStyleNotification(
			mediaSession: MediaSession,
			largeIcon: Bitmap?,
			channelId: String,
		): Notification {
			return NotificationCompat.Builder(musicLibrary, channelId)
				.apply {

					val player = mediaSession.player

					val contentIntent = PendingIntent
						.getActivity(musicLibrary, 445, AppDelegate.launcherIntent(),
							PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
						)

					val mediaItem = player.currentMediaItem.orEmpty()

					setChannelId(channelId)
					setContentIntent(contentIntent)
					setContentTitle(mediaItem.mediaMetadata.title)
					setContentText(mediaItem.mediaMetadata.artist)
					setSmallIcon(R.drawable.play_icon_theme3_notification)

					setColorized(true)
					setOngoing(false)
					setShowWhen(false)

					val subText = mediaItem.mediaMetadata.albumTitle
					if (!subText.isNullOrEmpty()) setSubText(subText)

					val repeatAction = when (player.repeatMode) {
						Player.REPEAT_MODE_OFF -> makeActionRepeatOffToOne(mediaItem)
						Player.REPEAT_MODE_ONE -> makeActionRepeatOneToAll(mediaItem)
						Player.REPEAT_MODE_ALL -> makeActionRepeatAllToOff(mediaItem)
						else -> throw IllegalStateException()
					}

					val prevAction =
						if (player.hasPreviousMediaItem()) {
							makeActionPrev(mediaItem)
						} else {
							makeActionPrevDisabled(mediaItem)
						}

					val showPlayAction =
						if (player.playbackState.isStateBuffering()) {
							!player.playWhenReady
						} else {
							!player.isPlaying
						}

					val playAction =
						if (showPlayAction) {
							makeActionPlay(mediaItem)
						} else {
							makeActionPause(mediaItem)
						}

					val nextAction =
						if (player.hasNextMediaItem()) {
							makeActionNext(mediaItem)
						} else {
							makeActionNextDisabled(mediaItem)
						}

					addAction(repeatAction)
					addAction(prevAction)
					addAction(playAction)
					addAction(nextAction)
					addAction(makeActionStopCancel(mediaItem))

					setLargeIcon(largeIcon)

					val dismissIntent = makeDismissPendingIntent(mediaItem)
					setDeleteIntent(dismissIntent)

					val style = MediaStyleNotificationHelper
						.DecoratedMediaCustomViewStyle(mediaSession)
						.setCancelButtonIntent(dismissIntent)
						.setShowCancelButton(true)
						.setShowActionsInCompactView(1,2,3)
					setStyle(style)

				}.build()
		}

		fun buildMediaStyleNotification(
			player: Player,
			largeIcon: Bitmap?,
			channelId: String,
		): Notification {
			return NotificationCompat.Builder(musicLibrary, channelId)
				.apply {

					val contentIntent = PendingIntent
						.getActivity(musicLibrary, 445, AppDelegate.launcherIntent(),
							PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
						)

					val mediaItem = player.currentMediaItem.orEmpty()

					setChannelId(channelId)
					setContentIntent(contentIntent)
					setContentTitle(mediaItem.mediaMetadata.title)
					setContentText(mediaItem.mediaMetadata.artist)
					setSmallIcon(R.drawable.play_icon_theme3_notification)

					setColorized(true)
					setOngoing(player.playbackState.isOngoing())
					setShowWhen(false)

					val subText = mediaItem.mediaMetadata.albumTitle
					if (!subText.isNullOrEmpty()) setSubText(subText)

					val repeatAction = when (player.repeatMode) {
						Player.REPEAT_MODE_OFF -> makeActionRepeatOffToOne(mediaItem)
						Player.REPEAT_MODE_ONE -> makeActionRepeatOneToAll(mediaItem)
						Player.REPEAT_MODE_ALL -> makeActionRepeatAllToOff(mediaItem)
						else -> throw IllegalStateException()
					}

					val prevAction =
						if (player.hasPreviousMediaItem()) {
							makeActionPrev(mediaItem)
						} else {
							makeActionPrevDisabled(mediaItem)
						}

					val showPlayAction =
						if (player.playbackState.isStateBuffering()) {
							!player.playWhenReady
						} else {
							!player.isPlaying
						}

					val playAction =
						if (showPlayAction) {
							makeActionPlay(mediaItem)
						} else {
							makeActionPause(mediaItem)
						}

					val nextAction =
						if (player.hasNextMediaItem()) {
							makeActionNext(mediaItem)
						} else {
							makeActionNextDisabled(mediaItem)
						}

					addAction(repeatAction)
					addAction(prevAction)
					addAction(playAction)
					addAction(nextAction)
					addAction(makeActionStopCancel(mediaItem))

					setLargeIcon(largeIcon)

					val dismissIntent = makeDismissPendingIntent(mediaItem)
					setDeleteIntent(dismissIntent)

					val style = androidx.media.app.NotificationCompat.MediaStyle()
						.setCancelButtonIntent(dismissIntent)
						.setShowCancelButton(true)
						.setShowActionsInCompactView(1,2,3)
					setStyle(style)

				}.build()
		}

		private fun makeDismissPendingIntent(item: MediaItem): PendingIntent {
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_DISMISS_NOTIFICATION)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			return PendingIntent.getBroadcast(musicLibrary, ACTION_DISMISS_NOTIFICATION_CODE, intent, flag)
		}

		private fun makeActionStopCancel(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_close
			val title = "ACTION_PLAY"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_STOP_CANCEL)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_STOP_CANCEL_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionRepeatOneToAll(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_repeat_one
			val title = "ACTION_PLAY"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ONE_TO_ALL)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_REPEAT_ONE_TO_ALL_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionRepeatOffToOne(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_repeat_off
			val title = "ACTION_REPEAT_OFF_TO_ONE"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_OFF_TO_ONE)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_REPEAT_OFF_TO_ONE_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionRepeatAllToOff(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_repeat_all
			val title = "ACTION_REPEAT_ALL_TO_OFF"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_REPEAT_ALL_TO_OFF)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_REPEAT_ALL_TO_OFF_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionPlay(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_play
			val title = "ACTION_PLAY"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PLAY)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_PLAY_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionPause(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_pause
			val title = "ACTION_PAUSE"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PAUSE)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_PAUSE_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionNext(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_next
			val title = "ACTION_NEXT"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_NEXT_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionNextDisabled(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_next_disabled
			val title = "ACTION_NEXT_DISABLED"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_NEXT_DISABLED)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_NEXT_DISABLED_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionPrev(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_prev
			val title = "ACTION_PREV"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_PREV_CODE, intent, flag)
			return NotificationCompat.Action.Builder(resId, title, pIntent).build()
		}

		private fun makeActionPrevDisabled(item: MediaItem): NotificationCompat.Action {
			val resId = R.drawable.ic_notif_prev_disabled
			val title = "ACTION_PREV_DISABLED"
			val intent = Intent(PLAYBACK_CONTROL_INTENT)
				.apply {
					putExtra(PLAYBACK_CONTROL_ACTION, ACTION_PREV_DISABLED)
					putExtra(PLAYBACK_ITEM_ID, item.mediaId)
					setPackage(musicLibrary.packageName)
				}
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val pIntent = PendingIntent.getBroadcast(musicLibrary, ACTION_PREV_DISABLED_CODE, intent, flag)
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
