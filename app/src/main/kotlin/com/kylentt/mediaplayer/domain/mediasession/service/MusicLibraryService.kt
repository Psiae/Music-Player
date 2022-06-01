package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.annotation.Singleton
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.connector.MediaServiceState
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryEventHandler
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryListener
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun interface WhenReady <T> {
	fun whenReady(ready: T)
}

fun interface OnChanged <T> {
	fun onChanged(old: T, new: T)
}

enum class ServiceLifecycleState {
	NOTHING,
	DESTROYED,
	ALIVE,
	FOREGROUND
}

@AndroidEntryPoint
@Singleton
class MusicLibraryService : MediaLibraryService() {

	fun interface OnDestroyCallback {
		fun onDestroy()
	}

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var coroutineDispatchers: AppDispatchers
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	private val whenSessionReady: MutableList<WhenReady<MediaLibrarySession>> = mutableListOf()
	private val onSessionPlayerChangedListener: MutableList<OnChanged<Player>> = mutableListOf()
	private val onDestroyCallback: MutableList<OnDestroyCallback> = mutableListOf()

	private val mediaLibrarySession: MediaLibrarySession by lazy {
		checkNotNull(baseContext)
		val intent = packageManager.getLaunchIntentForPackage(packageName)
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val sessionActivity = PendingIntent.getActivity(this, 444, intent, flag)
		with(MediaLibrarySession.Builder(this, injectedPlayer, sessionCallbackImpl)) {
			setId(Constants.SESSION_ID)
			setSessionActivity(sessionActivity)
			setMediaItemFiller(itemFillerImpl)
			build()
		}
	}

	val mediaEventHandler: MusicLibraryEventHandler by lazy {
		MusicLibraryEventHandler(this)
	}

	val mediaEventListener: MusicLibraryListener by lazy {
		MusicLibraryListener(this)
	}

	val mediaNotificationProvider: MusicLibraryNotificationProvider by lazy {
		MusicLibraryNotificationProvider(this)
	}

	private val itemFillerImpl = object : MediaSession.MediaItemFiller {
		override fun fillInLocalConfiguration(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			mediaItem: MediaItem
		) = mediaItemHelper.rebuildMediaItem(mediaItem)
	}

	private val sessionCallbackImpl = object : MediaLibrarySession.MediaLibrarySessionCallback {
		override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
			whenSessionReady.forEachClear { it.whenReady(mediaLibrarySession) }
			super.onPostConnect(session, controller)
		}
	}

	val currentMediaSession: MediaSession
		get() = mediaLibrarySession

	val mainScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob()) }
	val ioScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob())  }

	override fun onCreate() {
		super.onCreate()
		setMediaNotificationProvider(mediaNotificationProvider)
		mediaEventListener.init()
		return LifecycleState.updateState(this, ServiceLifecycleState.ALIVE)
	}
	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
		return mediaLibrarySession
	}

	override fun onDestroy() {
		Timber.d("MusicLibraryService onDestroy() is called")

		onDestroyCallback.forEachClear { it.onDestroy() }
		cancelServiceScope()
		releaseComponent()
		releaseSession()
		super.onDestroy()
		LifecycleState.updateState(this, ServiceLifecycleState.DESTROYED)
	}

	@MainThread
	private fun cancelServiceScope() {
		ioScope.cancel()
		mainScope.cancel()
	}

	@MainThread
	private fun releaseComponent() {
		if (injectedPlayer !== mediaLibrarySession.player) {
			injectedPlayer.release()
		}
		mediaLibrarySession.player.release()
		mediaLibrarySession.release()
		stopForegroundService(mediaNotificationProvider.mediaNotificationId, true)
	}

	@MainThread
	private fun releaseSession() {
		mediaLibrarySession.release()
	}

	@MainThread
	private fun changeSessionPlayer(player: Player) {
		checkMainThread()
		val get = currentMediaSession.player
		currentMediaSession.player = player
		onSessionPlayerChangedListener.forEach { it.onChanged(get, currentMediaSession.player) }
	}

	@MainThread
	fun startForegroundService(id: Int, notification: Notification) {
		checkMainThread()
		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}
		LifecycleState.updateState(this, ServiceLifecycleState.FOREGROUND)
	}

	@MainThread
	fun stopForegroundService(id: Int, removeNotification: Boolean) {
		checkMainThread()
		stopForeground(removeNotification)
		if (removeNotification) {
			mediaNotificationProvider.notificationManager.cancel(id)
		}
		LifecycleState.updateState(this, ServiceLifecycleState.ALIVE)
	}

	@MainThread
	fun registerOnPlayerChangedListener(listener: OnChanged<Player>) {
		checkMainThread()
		onSessionPlayerChangedListener.add(listener)
	}

	@MainThread
	fun unregisterOnPlayerChangedListener(listener: OnChanged<Player>): Boolean {
		checkMainThread()
		var removed = false
		onSessionPlayerChangedListener.removeAll {
			elseFalse(it === listener) {
				removed = true
				removed
			}
		}
		return removed
	}

	@MainThread
	fun registerOnDestroyCallback(callback: OnDestroyCallback) {
		checkMainThread()
		if (LifecycleState.isDestroyed()) {
			return callback.onDestroy()
		}
		onDestroyCallback.add(callback)
	}

	private inline fun elseFalse(condition: Boolean, block: () -> Boolean): Boolean {
		return if (condition) block.invoke() else false
	}

	@MainThread
	object LifecycleState : ReadOnlyProperty<Any?, ServiceLifecycleState> {

		private var currentState: ServiceLifecycleState = ServiceLifecycleState.NOTHING
		private var currentHashCode: Int? = null

		fun updateState(service: MediaLibraryService, state: ServiceLifecycleState) {
			when (state) {
				ServiceLifecycleState.ALIVE -> {
					currentHashCode = service.hashCode()
					currentState = state
				}
				ServiceLifecycleState.FOREGROUND -> {
					checkState(service.hashCode() == currentHashCode)
					currentState = state
				}
				ServiceLifecycleState.DESTROYED -> {
					if (service.hashCode() == currentHashCode) {
						currentHashCode = null
						currentState = state
					}
				}
				ServiceLifecycleState.NOTHING -> throw IllegalArgumentException()
			}
		}

		@JvmStatic fun wasLaunched() = currentState != ServiceLifecycleState.NOTHING
		@JvmStatic fun isDestroyed() = currentState == ServiceLifecycleState.DESTROYED
		@JvmStatic fun isAlive() = currentState == ServiceLifecycleState.ALIVE || isForeground()
		@JvmStatic fun isForeground() = currentState == ServiceLifecycleState.FOREGROUND

		override fun getValue(thisRef: Any?, property: KProperty<*>): ServiceLifecycleState = currentState
	}

	companion object {

		@JvmStatic fun getComponentName(packageName: Context) = ComponentName(packageName, MusicLibraryService::class.java)

		object Constants {
			const val SESSION_ID = "FLAMM"
		}
	}
}
