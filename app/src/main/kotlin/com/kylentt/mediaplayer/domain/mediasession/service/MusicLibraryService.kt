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
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryEventHandler
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryServiceListener
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun interface OnChanged <T> {
	fun onChanged(old: T, new: T)
}

fun interface OnDestroyCallback {
	fun onDestroy()
}

sealed class ServiceLifecycleState {
	object NOTHING : ServiceLifecycleState()
	object DESTROYED : ServiceLifecycleState()
	object ALIVE : ServiceLifecycleState()
	object FOREGROUND : ServiceLifecycleState()

	fun wasLaunched() = this !is NOTHING
	fun isAlive() = this is ALIVE || isForeground()
	fun isForeground() = this is FOREGROUND
	fun isDestroyed() = this is DESTROYED
}

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService() {

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var coroutineDispatchers: AppDispatchers
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	private val onSessionPlayerChangedListener: MutableList<OnChanged<Player>> = mutableListOf()
	private val onDestroyCallback: MutableList<OnDestroyCallback> = mutableListOf()

	private val mediaLibrarySession: MediaLibrarySession by lazy {
		checkNotNull(baseContext)
		val intent = packageManager.getLaunchIntentForPackage(packageName)
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val requestCode = MainActivity.Constants.LAUNCH_REQUEST_CODE
		val sessionActivity = PendingIntent.getActivity(this, requestCode, intent, flag)
		val builder = MediaLibrarySession.Builder(this, injectedPlayer, sessionCallbackImpl)
		with(builder) {
			setId(Constants.SESSION_ID)
			setSessionActivity(sessionActivity)
			setMediaItemFiller(itemFillerImpl)
			build()
		}
	}

	private val itemFillerImpl = object : MediaSession.MediaItemFiller {
		override fun fillInLocalConfiguration(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			mediaItem: MediaItem
		): MediaItem {
			return mediaItemHelper.rebuildMediaItem(mediaItem)
		}
	}

	private val sessionCallbackImpl = object : MediaLibrarySession.MediaLibrarySessionCallback {
		/* TODO */
	}

	val currentMediaSession: MediaSession
		get() = mediaLibrarySession

	val mediaEventHandler: MusicLibraryEventHandler = MusicLibraryEventHandler(this)

	val mediaEventListener: MusicLibraryServiceListener by lazy {
		MusicLibraryServiceListener(this)
	}

	val mediaNotificationProvider: MusicLibraryNotificationProvider by lazy {
		MusicLibraryNotificationProvider(this)
	}

	val mainScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob()) }
	val ioScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob())  }

	override fun onCreate() {
		super.onCreate()
		setMediaNotificationProvider(mediaNotificationProvider)
		mediaEventListener.init(true)
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
	fun startForegroundService(id: Int, notification: Notification) {
		checkMainThread()
		checkState(!LifecycleState.isForeground())
		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}
		Timber.d("MusicLibraryService StartForegroundService called")
		LifecycleState.updateState(this, ServiceLifecycleState.FOREGROUND)
	}

	@MainThread
	fun stopForegroundService(id: Int, removeNotification: Boolean) {
		checkMainThread()
		checkState(LifecycleState.isForeground())
		stopForeground(removeNotification)
		if (removeNotification) {
			mediaNotificationProvider.notificationManager.cancel(id)
		}
		Timber.d("MusicLibraryService StopForegroundService called")
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
		return onSessionPlayerChangedListener.removeAll { it === listener }
	}

	@MainThread
	fun registerOnDestroyCallback(callback: OnDestroyCallback) {
		if (LifecycleState.isDestroyed()) {
			return callback.onDestroy()
		}
		onDestroyCallback.add(callback)
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
		mediaEventListener.release()
	}

	@MainThread
	private fun releaseSession() {
		if (LifecycleState.isForeground()) {
			stopForegroundService(mediaNotificationProvider.mediaNotificationId, true)
		}
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
	object LifecycleState : ReadOnlyProperty<Any?, ServiceLifecycleState> {

		private var currentState: ServiceLifecycleState = ServiceLifecycleState.NOTHING
		private var currentHashCode: Int? = null

		fun updateState(service: MediaLibraryService, state: ServiceLifecycleState) {
			checkMainThread()
			when (state) {
				ServiceLifecycleState.ALIVE -> {
					currentHashCode = service.hashCode()
					currentState = state
				}
				ServiceLifecycleState.FOREGROUND -> {
					checkState(service.hashCode() == currentHashCode) {
						"ServiceLifecycleState Failed, currentHash: $currentHashCode, attempt: ${service.hashCode()}"
					}
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

		@JvmStatic
		fun getComponentName(packageName: Context): ComponentName {
			return ComponentName(packageName, MusicLibraryService::class.java)
		}
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}
}
