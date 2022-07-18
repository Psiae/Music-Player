package com.kylentt.musicplayer.domain.musiclib.service.manager

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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import coil.Coil
import com.google.common.collect.ImmutableList
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.common.coroutines.AutoCancelJob
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isRepeatAll
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isRepeatOff
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isRepeatOne
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.musicplayer.domain.musiclib.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.musicplayer.common.extenstions.checkCancellation
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemFactory
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemFactory.orEmpty
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemInfo
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.getDebugDescription
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService
import com.kylentt.musicplayer.domain.musiclib.service.provider.MediaNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.musicplayer.core.sdk.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.coroutineContext

class MediaNotificationManager(
	private val notificationId: Int
) : MusicLibraryService.ServiceComponent() {
	private lateinit var provider: Provider
	private lateinit var dispatcher: Dispatcher

	private lateinit var appDispatchers: CoroutineDispatchers
	private lateinit var mainScope: CoroutineScope

	private lateinit var coilHelper: CoilHelper
	private lateinit var notificationManagerService: NotificationManager

	private val eventListener = this.PlayerEventListener()
	val itemInfoIntentConverter = MediaItemInfo.IntentConverter()

	val isForegroundCondition: (
		MusicLibraryService.ComponentDelegate, MediaSession
	) -> Boolean = { delegate: MusicLibraryService.ComponentDelegate, session: MediaSession ->
		delegate.stateInteractor.getServiceForegroundCondition(session)
	}

	val interactor = Interactor()

	override fun create(serviceDelegate: MusicLibraryService.ServiceDelegate) {
		super.create(serviceDelegate)
		createImpl(serviceDelegate)
	}

	override fun serviceContextAttached(context: Context) {
		super.serviceContextAttached(context)
		initializeComponents(context)
	}

	override fun start(componentDelegate: MusicLibraryService.ComponentDelegate) {
		super.start(componentDelegate)
		startImpl(componentDelegate)
	}

	override fun release() {
		if (isReleased) return
		releaseImpl()
	}

	fun createImpl(serviceDelegate: MusicLibraryService.ServiceDelegate) {
		val serviceJob = serviceDelegate.propertyInteractor.serviceMainJob
		appDispatchers = serviceDelegate.propertyInteractor.serviceDispatchers
		mainScope = CoroutineScope(appDispatchers.main + serviceJob)
	}

	fun startImpl(componentDelegate: MusicLibraryService.ComponentDelegate) {
		componentDelegate.sessionInteractor.registerPlayerEventListener(eventListener)
	}

	fun releaseImpl() {
		if (::mainScope.isInitialized) mainScope.cancel()
		if (isComponentInitialized) {
			dispatcher.release()
			provider.release()
		}
		if (isStarted) {
			componentDelegate.sessionInteractor.removePlayerEventListener(eventListener)
		}
	}

	fun getNotification(
		session: MediaSession,
		onGoing: Boolean
	): Notification = provider.fromMediaSession(session, onGoing, ChannelName)

	fun startForegroundService(
		session: MediaSession,
		onGoingNotification: Boolean
	) {
		if (!isStarted) return
		val notification = getNotification(session, onGoingNotification)
		componentDelegate.stateInteractor.startForegroundService(notification)
	}

	fun stopForegroundService(removeNotification: Boolean) {
		if (!isStarted) return
		componentDelegate.stateInteractor.stopForegroundService(removeNotification)
	}

	fun getProvider(): MediaNotification.Provider = this.provider

	fun onUpdateNotification(session: MediaSession) {
		if (!isStarted) return
		val foregroundCondition = isForegroundCondition(componentDelegate, session)
		val notification = provider.fromMediaSession(session, foregroundCondition, ChannelName)
		dispatcher.updateNotification(notificationId, notification)
	}

	private var isComponentInitialized = false

	private fun initializeComponents(context: Context) {
		notificationManagerService = context.getSystemService(NotificationManager::class.java)!!
		coilHelper = CoilHelper(context.applicationContext, Coil.imageLoader(context.applicationContext))
		initializeProvider(context)
		initializeDispatcher(context)
		isComponentInitialized = true
	}

	private fun initializeProvider(context: Context) {
		checkState(!::provider.isInitialized)
		provider = Provider(context)
	}

	private fun initializeDispatcher(context: Context) {
		checkState(!::dispatcher.isInitialized)
		dispatcher = Dispatcher()
	}

	private inner class Dispatcher() {

		var isReleased: Boolean = false
			private set

		init {
			if (VersionHelper.hasOreo()) {
				createNotificationChannel(notificationManagerService)
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

			notificationManagerService.notify(id, notification)
		}

		suspend fun suspendUpdateNotification(id: Int, notification: Notification) {
			if (isReleased) return

			coroutineContext.ensureActive()
			notificationManagerService.notify(id, notification)
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
					notificationManagerService.notify(id, getNotification())
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

			Timber.d("getNotificationFromPlayer for "
				+ player.currentMediaItem.orEmpty().getDebugDescription())

			val largeIcon = getItemBitmap(player)
			return notificationProvider.buildMediaStyleNotification(
				player,
				largeIcon,
				onGoing,
				channelName
			)
		}

		private fun getNotificationFromMediaSession(
			session: MediaSession,
			onGoing: Boolean,
			channelName: String
		): Notification {

			Timber.d("getNotificationFromMediaSession for " +
				session.player.currentMediaItem.orEmpty().getDebugDescription())

			val largeIcon = getItemBitmap(session.player)
			return notificationProvider.buildMediaStyleNotification(
				session,
				largeIcon,
				onGoing,
				channelName
			)
		}

		suspend fun updateItemBitmap(player: Player) =
			withContext(this@MediaNotificationManager.appDispatchers.main) {
				if (!isStarted) return@withContext

				val item = player.currentMediaItem.orEmpty()
				withContext(this@MediaNotificationManager.appDispatchers.io) updateValue@{
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
					val squaredBitmap = coilHelper.squareBitmap(bitmap, 500)

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
				getNotificationFromMediaSession(session,
					isForegroundCondition(componentDelegate, session), ChannelName
				)

			dispatcher.cancelValidatorJob()

			return MediaNotification(notificationId, notification)
		}

		inner class NotificationActionReceiver : MediaNotificationProvider.ActionReceiver {

			override fun actionAny(context: Context, intent: Intent): Int {
				if (!isStarted) return -1

				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						if (player.mediaItemCount == 0) {
							notificationManagerService.cancel(notificationId)

							val itemInfo: MediaItemInfo? =
								if (itemInfoIntentConverter.isConvertible(intent)) {
									itemInfoIntentConverter.toMediaItemInfo(intent)
								} else {
									null
								}

							if (itemInfo != null && itemInfo.mediaItem.mediaUri != null) {
								// TODO: handle this
								Timber.w(
									"received Notification action when playback item is empty (convertible)")
							} else {
								// TODO: handle this
								Timber.w(
									"received Notification action when playback item is empty")
							}
							return -1
						}
					}
				return 0
			}

			override fun actionPlay(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							player.playbackState.isStateIdle() -> player.prepare()

							player.playbackState.isStateEnded() -> {
								if (player.hasNextMediaItem()) player.seekToNextMediaItem()
								player.seekTo(0)
							}

							player.playbackState.isOngoing() && player.playWhenReady -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.play()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionPause(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							player.playbackState.isStateIdle() -> player.prepare()
							player.playbackState.isOngoing() && !player.playWhenReady -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.pause()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionNext(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							!player.hasNextMediaItem() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.seekToNextMediaItem()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionPrevious(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							!player.hasPreviousMediaItem() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.seekToPreviousMediaItem()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatOffToOne(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatOff() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_ONE
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatOneToAll(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatOne() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_ALL
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionRepeatAllToOff(context: Context, intent: Intent) {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val player = it.player

						when {
							!player.repeatMode.isRepeatAll() -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						player.repeatMode = Player.REPEAT_MODE_OFF
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionNoNext(context: Context, intent: Intent) = Unit

			override fun actionNoPrevious(context: Context, intent: Intent) = Unit

			override fun actionStop(context: Context, intent: Intent) {
				if (!isStarted) return

				componentDelegate.sessionInteractor.mediaSession
					?.let {

						when {
							!isForegroundCondition(componentDelegate, it) -> {
								val notification = provider
									.getNotificationFromMediaSession(it, false, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						dispatcher.cancelValidatorJob()

						it.player.pause()
						it.player.stop()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionCancel(context: Context, intent: Intent) {
				if (!isStarted) return
				val stateInteractor = componentDelegate.stateInteractor

				componentDelegate.sessionInteractor.mediaSession
					?.let {

						when {
							isForegroundCondition(componentDelegate, it) -> {
								val notification = provider
									.getNotificationFromMediaSession(it, true, ChannelName)
								return dispatcher.updateNotification(notificationId, notification)
							}
						}

						dispatcher.cancelValidatorJob()

						stateInteractor.stopForegroundService(true)
						stateInteractor.stopService()
						if (!MainActivity.isAlive) stateInteractor.releaseService()
					}
					?: throw IllegalStateException("Received Intent: $intent on Released State")
			}

			override fun actionDismiss(context: Context, intent: Intent) =
				actionCancel(context, intent) // placeholder
		}

	}

	private inner class PlayerEventListener : Player.Listener {

		private var mediaItemTransitionJob by AutoCancelJob()
		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			if (!isStarted) return

			mediaItemTransitionJob = mainScope.launch {
				componentDelegate.sessionInteractor.mediaSession
					?.let {
						provider.updateItemBitmap(it.player)
						val getNotification = {
							provider.fromMediaSession(it, isForegroundCondition(componentDelegate, it), ChannelName)
						}
						dispatcher.suspendUpdateNotification(notificationId, getNotification())
						dispatcher.dispatchNotificationValidator(
							notificationId,
							getNotification = getNotification
						)
					}
			}
		}

		private var playerRepeatModeJob by AutoCancelJob()
		override fun onRepeatModeChanged(repeatMode: Int) {
			if (!isStarted) return

			playerRepeatModeJob = mainScope.launch {

				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val getNotification = {
							provider.fromMediaSession(it, isForegroundCondition(componentDelegate, it), ChannelName)
						}
						dispatcher.suspendUpdateNotification(notificationId, getNotification())
						dispatcher.dispatchNotificationValidator(
							notificationId,
							getNotification = getNotification
						)
				}
			}
		}

		private var playWhenReadyChangedJob by AutoCancelJob()
		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			if (!isStarted) return

			playWhenReadyChangedJob = mainScope.launch {

				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val getNotification = {
							provider.fromMediaSession(it, isForegroundCondition(componentDelegate, it), ChannelName)
						}
						dispatcher.dispatchNotificationValidator(
							notificationId,
							getNotification = getNotification
						)
					}
			}
		}

		private var playbackStateChangedJob by AutoCancelJob()
		override fun onPlaybackStateChanged(playbackState: Int) {
			if (!isStarted) return

			playbackStateChangedJob = mainScope.launch {

				componentDelegate.sessionInteractor.mediaSession
					?.let {
						val getNotification = {
							val shouldForeground = isForegroundCondition(componentDelegate, it)
							val notification = provider.fromMediaSession(it, shouldForeground, ChannelName)
							notification
						}
						dispatcher.dispatchNotificationValidator(notificationId, getNotification = getNotification)
					}
			}
		}

	}

	inner class Interactor {

		fun getNotification(
			session: MediaSession,
			onGoing: Boolean
		): Notification = this@MediaNotificationManager.getNotification(session, onGoing)

		fun startForegroundService(
			mediaSession: MediaSession,
			onGoingNotification: Boolean
		) {
			this@MediaNotificationManager
				.startForegroundService(mediaSession, onGoingNotification)
		}

		fun stopForegroundService(removeNotification: Boolean) {
			this@MediaNotificationManager
				.stopForegroundService(removeNotification)
		}
	}

	companion object {
		const val ChannelName = "MusicLibrary Channel"
		const val NotificationName = "Playback"
	}
}
