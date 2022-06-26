package com.kylentt.mediaplayer.domain.mediasession.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.delegates.AutoCancelJob
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.extenstions.LifecycleService
import com.kylentt.mediaplayer.core.extenstions.checkCancellation
import com.kylentt.mediaplayer.core.media3.MediaItemFactory
import com.kylentt.mediaplayer.core.media3.MediaItemFactory.orEmpty
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemInfo
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand.Companion.wrapWithFadeOut
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext
import kotlin.system.exitProcess

class MediaNotificationManager(
	private val musicLibrary: MusicLibraryService,
	private val sessionDelegate: MusicLibrarySessionManager.Delegator
) {

	private lateinit var provider: Provider
	private lateinit var dispatcher: Dispatcher

	private val eventListener = this.PlayerEventListener()

	val notificationManager
		get() = musicLibrary.notificationManager

	val appDispatchers = AppModule.provideAppDispatchers()
	val mainScope = CoroutineScope(appDispatchers.main + musicLibrary.serviceJob)
	val immediateScope = CoroutineScope(appDispatchers.mainImmediate + musicLibrary.serviceJob)

	val itemInfoIntentConverter = MediaItemInfo.IntentConverter()
	val isForegroundInternal: (Player) -> Boolean = { it.playWhenReady }
	val isForegroundCondition: (Player) -> Boolean = { it.playbackState.isOngoing() }

	init {
		checkState(musicLibrary.sessions.isEmpty()) {
			"This Provider must be initialized before MediaLibraryService.onGetSession()" }

		sessionDelegate.registerPlayerEventListener(eventListener)
		initializeWhenContextReady()
	}

	var isReleased: Boolean = false
		private set

	fun release(obj: Any) {
		Timber.d("$obj called MediaNotificationManager.release()")

		val isProviderInitialized = ::provider.isInitialized
		val isDispatcherInitialized = ::dispatcher.isInitialized

		if (isReleased) {
			val a = if (isProviderInitialized) provider.isReleased else true
			val b = if (isDispatcherInitialized) dispatcher.isReleased else true
			val c = !mainScope.isActive

			checkState(a && b && c)
			return
		}

		mainScope.cancel()
		if (isProviderInitialized) provider.release()
		if (isDispatcherInitialized) dispatcher.release()

		isReleased = true
	}

	fun getProvider(): MediaNotification.Provider = this.provider

	fun onUpdateNotification(session: MediaSession) {
		Timber.d("onUpdateNotification")

		val notification =
			provider.fromMediaSession(session, isForegroundCondition(session.player), ChannelName)
		dispatcher.updateNotification(NotificationId, notification)
	}

	private fun initializeWhenContextReady() {

		val owner = musicLibrary as LifecycleService

		val obs = object : LifecycleEventObserver {

			override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
				Timber.d("MusicLibraryService onStateChanged,\nstate: ${source.lifecycle.currentState}\nevent: $event")

				when {
					event == Lifecycle.Event.ON_DESTROY -> {
						release(this)
						owner.lifecycle.removeObserver(this)
					}
					(event == Lifecycle.Event.ON_CREATE
						|| owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) && !isInitialized -> {
						initializeComponents(owner.service)
					}
					isReleased -> owner.lifecycle.removeObserver(this)
				}
			}
		}

		owner.lifecycle.addObserver(obs)
	}

	private var isInitialized = false

	fun initializeComponents(context: Context) {
		initializeProvider(context)
		initializeDispatcher()
		isInitialized = true
	}

	private fun initializeProvider(context: Context) {
		checkState(!::provider.isInitialized)
		provider = Provider(context)
	}

	private fun initializeDispatcher() {
		checkState(!::dispatcher.isInitialized)
		dispatcher = Dispatcher()
	}

	private inner class Dispatcher {

		var isReleased: Boolean = false
			private set

		init {
			if (VersionHelper.hasOreo()) {
				createNotificationChannel(notificationManager)
			}
		}

		fun release() {
			if (isReleased) {
				return
			}

			isReleased = true
			Timber.d("MediaNotificationManager.Dispatcher released")
		}

		fun updateNotification(id: Int, notification: Notification) {
			if (isReleased) return

			notificationManager.notify(id, notification)
		}

		suspend fun suspendUpdateNotification(id: Int, notification: Notification) {
			if (isReleased) return

			coroutineContext.ensureActive()
			notificationManager.notify(id, notification)
		}

		fun considerForegroundService(id: Int, notification: Notification, shouldForeground: Boolean) {
			if (isReleased) return

			when {
				shouldForeground -> {
					if (musicLibrary.isServiceForeground) return
					musicLibrary.startForegroundService(id, notification, isEvent = true)
				}
				else -> {
					if (!musicLibrary.isServiceForeground) return
					musicLibrary.stopForegroundService(id, false, isEvent = true)
				}
			}
		}

		private var validatorJob by AutoCancelJob()
		suspend fun dispatchNotificationValidator(
			id: Int,
			delay: Long = 500,
			repeat: Int = 2,
			getNotification: () -> Notification
		) {
			if (isReleased) return
			coroutineContext.ensureActive()
			validatorJob = mainScope.launch {
				repeat(repeat) {
					delay(delay)
					coroutineContext.ensureActive()
					notificationManager.notify(id, getNotification())
				}
			}
		}

		fun cancelValidatorJob(): Unit {
			if (isReleased) return
			validatorJob.cancel()
		}

		@RequiresApi(VERSION_CODES.O)
		private fun createNotificationChannel(manager: NotificationManager) {
			val importance = NotificationManager.IMPORTANCE_LOW
			val channel = NotificationChannel(ChannelName, NotificationName, importance)
			manager.createNotificationChannel(channel)
		}
	}

	private inner class Provider(private val context: Context) : MediaNotification.Provider {

		private var currentItemBitmap: Pair<MediaItem, Bitmap?> = MediaItem.EMPTY to null
		private val notificationProvider = MediaNotificationProvider(context, itemInfoIntentConverter)

		init {
			notificationProvider.setActionReceiver(NotificationActionReceiver())
		}

		var isReleased: Boolean = false
			private set

		fun release() {
			if (isReleased) {
				checkState(notificationProvider.isReleased
					&& currentItemBitmap == MediaItem.EMPTY to null)
				return
			}

			notificationProvider.release()
			currentItemBitmap = MediaItem.EMPTY to null

			isReleased = true
			Timber.d("MediaNotificationManager.Provider released()")
		}

		override fun createNotification(
			session: MediaSession,
			customLayout: ImmutableList<CommandButton>,
			actionFactory: MediaNotification.ActionFactory,
			onNotificationChangedCallback: MediaNotification.Provider.Callback
		): MediaNotification = createNotificationInternalImpl(session)

		override fun handleCustomCommand(
			session: MediaSession,
			action: String,
			extras: Bundle
		): Boolean = false


		fun fromMediaSession(
			session: MediaSession,
			onGoing: Boolean,
			channelName: String
		): Notification = getNotificationFromMediaSession(session, onGoing, channelName)

		private fun getNotificationFromPlayer(
			player: Player,
			onGoing: Boolean,
			channelName: String
		): Notification {

			Timber.d("getNotificationFromPlayer for " +
				player.currentMediaItem.orEmpty().getDebugDescription()
			)

			val largeIcon = getItemBitmap(player)
			return notificationProvider.buildMediaStyleNotification(player, largeIcon, onGoing, channelName)
		}

		private fun getNotificationFromMediaSession(
			session: MediaSession,
			onGoing: Boolean,
			channelName: String
		): Notification {

			Timber.d("getNotificationFromMediaSession for " +
				session.player.currentMediaItem.orEmpty().getDebugDescription()
			)

			val largeIcon = getItemBitmap(session.player)
			return notificationProvider.buildMediaStyleNotification(session, largeIcon, onGoing, channelName)
		}

		suspend fun updateItemBitmap(player: Player) = withContext(appDispatchers.main) {
			val item = player.currentMediaItem.orEmpty()

			 withContext(appDispatchers.io) updateValue@ {
				 val bitmap = MediaItemFactory.getEmbeddedImage(context, item)
					 ?.let {
						 ensureActive()
						 BitmapFactory.decodeByteArray(it, 0, it.size)
					 }
					 ?: run {
						 currentItemBitmap = item to null
						 return@updateValue
					 }

				 checkCancellation { bitmap.recycle() }
				 // maybe create Fitter Class for some APIs version or Device that require some modification
				 // to have proper display
				 val squaredBitmap = musicLibrary.coilHelper.squareBitmap(bitmap, 500)

				 checkCancellation { bitmap.recycle() }
				 currentItemBitmap = item to squaredBitmap
			}
		}

		private fun getItemBitmap(player: Player): Bitmap? {
			return if (player.currentMediaItem?.mediaId == currentItemBitmap.first.mediaId) {
				currentItemBitmap.second
			} else {
				null
			}
		}

		private fun createNotificationInternalImpl(
			session: MediaSession
		): MediaNotification {
			Timber.d("createNotificationInternalImpl")

			val notification =
				getNotificationFromPlayer(session.player, isForegroundCondition(session.player), ChannelName)

			dispatcher.cancelValidatorJob()

			return MediaNotification(NotificationId, notification)
		}

		private fun handleInternalForeground(
			player: Player,
			notification: Notification,
			wasForeground: Boolean
		) {
			val isForeground = isForegroundInternal(player)
			when {
				wasForeground && isForeground && !musicLibrary.isServiceForeground -> {
					musicLibrary.startForegroundService(NotificationId, notification, true)
				}
				!wasForeground && !isForeground && musicLibrary.isServiceForeground -> {
					musicLibrary.stopForegroundService(NotificationId, removeNotification = false, isEvent = true)
				}
			}
		}

		inner class NotificationActionReceiver : MediaNotificationProvider.ActionReceiver {

			override fun actionPlay(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							player.mediaItemCount == 0 -> {

								val itemInfo: MediaItemInfo? =
									if (itemInfoIntentConverter.isConvertible(intent)) {
										itemInfoIntentConverter.toMediaItemInfo(intent)
									} else {
										null
									}

								if (itemInfo != null && itemInfo.mediaItem.mediaUri != null) {
									player.setMediaItem(itemInfo.mediaItem)
									player.prepare()
									player.play()
								} else {
									// Later
									exitProcess(1)
								}
							}

							player.playbackState.isStateIdle() -> player.prepare()

							player.playbackState.isStateEnded() -> {
								if (player.hasNextMediaItem()) player.seekToNextMediaItem()
								player.seekTo(0)
							}

							player.playbackState.isOngoing() && player.playWhenReady -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.play()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionPause(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							player.playbackState.isStateIdle() -> player.prepare()
							player.playbackState.isOngoing() && !player.playWhenReady -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.pause()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionNext(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!player.hasNextMediaItem() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.seekToNextMediaItem()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionPrevious(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!player.hasPreviousMediaItem() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.seekToPreviousMediaItem()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatOffToOne(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatOff() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_ONE
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatOneToAll(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatOne() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_ALL
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatAllToOff(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatAll() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_OFF
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionNoNext(context: Context, intent: Intent) = Unit

			override fun actionNoPrevious(context: Context, intent: Intent) = Unit

			override fun actionStop(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							!isForegroundCondition(player) -> {
								val notification = provider
									.getNotificationFromMediaSession(it, false, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						dispatcher.cancelValidatorJob()

						val commands = listOf(
							ControllerCommand.SetPlayWhenReady(false),
							ControllerCommand.STOP
						)

						val command =
							ControllerCommand.MultiCommand(commands).wrapWithFadeOut(true, 500, 50)
						musicLibrary.sessionConnector.sendControllerCommand(command)
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionCancel(context: Context, intent: Intent) {
				sessionDelegate.mediaSession
					?.let {
						val player = it.player

						when {
							isForegroundCondition(player) -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(NotificationId, notification)
							}
						}

						dispatcher.cancelValidatorJob()

						musicLibrary.stopForegroundService(NotificationId, true, isEvent = false)
						musicLibrary.stopService(releaseSession = !MainActivity.isAlive)
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionDismiss(context: Context, intent: Intent) = actionCancel(context, intent) // placeholder
		}

	}

	private inner class PlayerEventListener : Player.Listener {

		private var mediaItemTransitionJob by AutoCancelJob()
		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			mediaItemTransitionJob = immediateScope.launch {

				sessionDelegate.mediaSession?.let {
					provider.updateItemBitmap(it.player)
					val getNotification = {
						provider.fromMediaSession(it, isForegroundCondition(it.player), ChannelName)
					}
					dispatcher.dispatchNotificationValidator(NotificationId, getNotification = getNotification)
				}
			}
		}

		private var playerRepeatModeJob by AutoCancelJob()
		override fun onRepeatModeChanged(repeatMode: Int) {

			playerRepeatModeJob = mainScope.launch {

				sessionDelegate.mediaSession?.let {
					val getNotification = {
						provider.fromMediaSession(it, isForegroundCondition(it.player), ChannelName)
					}
					dispatcher.suspendUpdateNotification(NotificationId, getNotification())
					dispatcher.dispatchNotificationValidator(NotificationId, getNotification = getNotification)
				}
			}
		}

		private var playWhenReadyChangedJob by AutoCancelJob()
		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {

			playWhenReadyChangedJob = mainScope.launch {

				sessionDelegate.mediaSession?.let {
					val getNotification = {
						provider.fromMediaSession(it, isForegroundCondition(it.player), ChannelName)
					}
					dispatcher.dispatchNotificationValidator(NotificationId, getNotification = getNotification)
				}
			}
		}

		private var playbackStateChangedJob by AutoCancelJob()
		override fun onPlaybackStateChanged(playbackState: Int) {

			playbackStateChangedJob = mainScope.launch {

				sessionDelegate.mediaSession?.let {
					val getNotification = {
						val shouldForeground = isForegroundCondition(it.player)
						val notification = provider.fromMediaSession(it, shouldForeground, ChannelName)
						dispatcher.considerForegroundService(NotificationId, notification, shouldForeground)
						notification
					}
					dispatcher.dispatchNotificationValidator(NotificationId, getNotification = getNotification)
				}
			}
		}

	}

	companion object {
		const val ChannelName = "MusicLibrary Channel"
		const val NotificationName = "Playback"
		const val NotificationId = 301
	}
}
