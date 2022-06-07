package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback
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

/*
lateinit
Private val's
Private vars
Private backed val's
Private backed vars
Private anon object val's
Private anon object vars
Public's

Override fun's
Private fun's
Public fun's
*/

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService(), LifecycleOwner {

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var coroutineDispatchers: AppDispatchers
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	private val onPlayerChangedListener: MutableList<OnChanged<Player>> = mutableListOf()
	private val onMediaSessionChangedListener: MutableList<OnChanged<MediaSession>> = mutableListOf()

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

	private val mediaLibrarySession: MediaLibrarySession by lazy {
		checkNotNull(baseContext)
		val intent = packageManager.getLaunchIntentForPackage(packageName)
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val requestCode = MainActivity.Constants.LAUNCH_REQUEST_CODE
		val sessionActivity = PendingIntent.getActivity(this, requestCode, intent, flag)
		val builder = MediaLibrarySession.Builder(this, injectedPlayer, SessionCallbackImpl())
		with(builder) {
			setId(Constants.SESSION_ID)
			setSessionActivity(sessionActivity)
			setMediaItemFiller(MediaItemFillerImpl())
			build()
		}
	}

	private val mediaNotificationProvider: MusicLibraryNotificationProvider by lazy {
		MusicLibraryNotificationProvider(this)
	}
	private val mediaEventHandler by lazy {
		MusicLibraryEventHandler(this, mediaNotificationProvider)
	}
	private val mediaEventListener by lazy {
		MusicLibraryServiceListener(this, mediaEventHandler)
	}

	private var isForegroundService = false

	val mainScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob()) }
	val ioScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob())  }

	val currentMediaSession: MediaSession
		get() = mediaLibrarySession

	init {
		dispatchLifecycleStateObjectUpdater()
	}

	override fun onCreate() {
		Timber.d("Service onCreate()")

		lifecycleRegistry.handleLifecycleEvent(ON_CREATE)
		super.onCreate()

		setupNotificationProvider()
		setupEventListener()
	}

	override fun onBind(intent: Intent?): IBinder? {
		Timber.d("Service onBind()")
		lifecycleRegistry.handleLifecycleEvent(ON_START)
		return super.onBind(intent)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Timber.d("Service onStartCommand(), ${intent}\n${flags}\n${startId}")
		return super.onStartCommand(intent, flags, startId)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
		Timber.d("Service onGetSession()")
		return mediaLibrarySession
	}

	override fun onDestroy() {
		Timber.d("MusicLibraryService onDestroy() is called")
		lifecycleRegistry.handleLifecycleEvent(ON_DESTROY)
		cancelServiceScope()
		releaseComponent()
		releaseSession()
		super.onDestroy()
	}

	override fun getLifecycle(): Lifecycle {
		return lifecycleRegistry
	}

	@MainThread
	private fun dispatchLifecycleStateObjectUpdater() {
		val registry = lifecycleRegistry
		checkState(registry.currentState == INITIALIZED)
		LifecycleStateDelegate.updateState(this, LifecycleState.INITIALIZED)

		val eventObserver = LifecycleEventObserver { _, event ->
			when (event) {
				ON_CREATE -> LifecycleStateDelegate.updateState(this, LifecycleState.CREATED)
				ON_START -> LifecycleStateDelegate.updateState(this, LifecycleState.STARTED)
				ON_RESUME -> {
					checkState(isForegroundService)
					LifecycleStateDelegate.updateState(this, LifecycleState.RESUMED(true))
				}
				ON_PAUSE -> {
					checkState(!isForegroundService)
					LifecycleStateDelegate.updateState(this, LifecycleState.RESUMED(false))
				}
				ON_DESTROY -> LifecycleStateDelegate.updateState(this, LifecycleState.DESTROYED)
				else -> Unit
			}
		}

		lifecycleRegistry.addObserver(eventObserver)
	}

	private fun setupNotificationProvider() {
		setMediaNotificationProvider(mediaNotificationProvider)
	}

	private fun setupEventListener() {
		mediaEventListener.start(true)
	}

	@MainThread
	fun startForegroundService(id: Int, notification: Notification, ignoreCheck: Boolean = false) {
		checkMainThread()
		if (!ignoreCheck) {
			checkState(!LifecycleStateDelegate.isForeground())
		}
		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}
		Timber.d("MusicLibraryService StartForegroundService called")
		isForegroundService = true
		lifecycleRegistry.handleLifecycleEvent(ON_RESUME)
	}

	@MainThread
	fun stopForegroundService(id: Int, removeNotification: Boolean, ignoreCheck: Boolean = false) {
		checkMainThread()
		if (!ignoreCheck) {
			// should be called by Notification Validator in case the foreground is started internally
			checkState(LifecycleStateDelegate.isForeground())
		}
		stopForeground(removeNotification)
		if (removeNotification) {
			mediaNotificationProvider.notificationManager.cancel(id)
		}
		Timber.d("MusicLibraryService StopForegroundService called")
		isForegroundService = false
		lifecycleRegistry.handleLifecycleEvent(ON_PAUSE)
	}

	@MainThread
	fun registerOnPlayerChangedListener(listener: OnChanged<Player>) {
		checkMainThread()
		onPlayerChangedListener.add(listener)
	}

	@MainThread
	fun unRegisterOnPlayerChangedListener(listener: OnChanged<Player>): Boolean {
		checkMainThread()
		return onPlayerChangedListener.removeAll { it === listener }
	}

	@MainThread
	fun registerOnMediaSessionChangedListener(listener: OnChanged<MediaSession>): Boolean {
		checkMainThread()
		return onMediaSessionChangedListener.add(listener)
	}

	@MainThread
	fun unRegisterOnMediaSessionChangedListener(listener: OnChanged<MediaSession>): Boolean {
		checkMainThread()
		return onMediaSessionChangedListener.removeAll { it === listener }
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
		if (LifecycleStateDelegate.isForeground()) {
			stopForegroundService(mediaNotificationProvider.mediaNotificationId, true)
		}
		try {
			mediaLibrarySession.release()
		} catch (e: NullPointerException) {
			// NPE in MediaControllerImplBase.java:3041 when calling librarySession.release()
		}
	}

	@MainThread
	private fun changeSessionPlayer(player: Player) {
		checkMainThread()
		val get = currentMediaSession.player
		currentMediaSession.player = player
		onPlayerChangedListener.forEach { it.onChanged(get, currentMediaSession.player) }
	}

	inner class SessionCallbackImpl : MediaLibrarySessionCallback {
		override fun onConnect(
			session: MediaSession,
			controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			Timber.i("SessionCallbackImpl onConnect pre Super")
			return super.onConnect(session, controller)
		}

		override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
			super.onPostConnect(session, controller)
			Timber.i("SessionCallbackImpl onPostConnect post Super")
		}

		override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
			super.onDisconnected(session, controller)
			Timber.i("SessionCallbackImpl onDisconnected post Super")
		}
	}

	inner class MediaItemFillerImpl : MediaSession.MediaItemFiller {
		override fun fillInLocalConfiguration(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			mediaItem: MediaItem
		): MediaItem {
			return mediaItemHelper.rebuildMediaItem(mediaItem)
		}
	}

	inner class StateRegistry {



	}

	companion object {
		@JvmStatic
		fun getComponentName(packageName: Context): ComponentName {
			return ComponentName(packageName, MusicLibraryService::class.java)
		}
	}

	sealed class LifecycleState {
		object NOTHING : LifecycleState()
		// Init
		object INITIALIZED : LifecycleState()
		// OnCreate pre-Super
		object CREATED : LifecycleState()
		// OnBind pre-Super
		object STARTED : LifecycleState()
		// ForegroundService
		data class RESUMED(val isForeground: Boolean = false) : LifecycleState()

		object DESTROYED: LifecycleState()

		fun isForegroundService() = this is RESUMED && isForeground
	}

	sealed class MediaState {
		object UNINITIALIZED : MediaState()
		object INITIALIZED : MediaState()
		object CONNECTING : MediaState()
		object CONNECTED : MediaState()
		object DISCONNECTED : MediaState()
		data class ERROR (val e: Exception, val msg: String)
	}

	@MainThread
	object LifecycleStateDelegate : ReadOnlyProperty<Any?, LifecycleState> {

		private var currentState: LifecycleState = LifecycleState.NOTHING
		private var currentHashCode: Int? = null

		fun updateState(service: MediaLibraryService, state: LifecycleState) {
			checkMainThread()
			when (state) {
				LifecycleState.INITIALIZED -> currentHashCode = service.hashCode()
				LifecycleState.NOTHING -> throw IllegalArgumentException()
				else -> Unit
			}
			checkState(service.hashCode() == currentHashCode) {
				"ServiceLifecycleState Failed, currentHash: $currentHashCode, attempt: ${service.hashCode()}"
			}
			if (currentState != state) {
				currentState = state
				Timber.d("ServiceLifecycleState updated to $state")
			}
		}

		@JvmStatic fun wasLaunched() = currentState != LifecycleState.NOTHING
		@JvmStatic fun isDestroyed() = currentState == LifecycleState.DESTROYED
		@JvmStatic fun isAlive() = wasLaunched() && !isDestroyed()
		@JvmStatic fun isForeground(): Boolean {
			val state = currentState
			return (state is LifecycleState.RESUMED) && state.isForeground
		}

		override fun getValue(thisRef: Any?, property: KProperty<*>): LifecycleState = currentState
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}
}
