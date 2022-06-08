package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.Lifecycle.State.*
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun interface OnChanged <T> {
	fun onChanged(old: T, new: T)
}

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService() {

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var coroutineDispatchers: AppDispatchers
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	private val onPlayerChangedListener: MutableList<OnChanged<Player>> = mutableListOf()
	private val onMediaSessionChangedListener: MutableList<OnChanged<MediaSession>> = mutableListOf()

	private val stateRegistry = StateRegistry()

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

	private val mediaNotificationProvider by lazy {
		MusicLibraryNotificationProvider(this)
	}
	private val mediaEventHandler by lazy {
		MusicLibraryEventHandler(this, mediaNotificationProvider)
	}
	private val mediaEventListener by lazy {
		MusicLibraryServiceListener(this, mediaEventHandler)
	}

	private val _serviceStateSF: MutableStateFlow<STATE> = MutableStateFlow(STATE.NOTHING)
	private val _mediaStateSF: MutableStateFlow<MediaState> = MutableStateFlow(MediaState.NOTHING)

	val serviceStateSF = _serviceStateSF.asStateFlow()
	val mediaStateSF = _mediaStateSF.asStateFlow()

	val mainScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob()) }
	val ioScope by lazy { CoroutineScope(coroutineDispatchers.main + SupervisorJob())  }

	val currentMediaSession: MediaSession
		get() = mediaLibrarySession

	init {}

	override fun onCreate() {
		Timber.d("Service onCreate()")
		stateRegistry.triggerEvent(EVENT.ON_CREATE)
		super.onCreate()

		setupEventListener()
		setupNotificationProvider()
	}

	override fun onBind(intent: Intent?): IBinder? {
		Timber.d("Service onBind()")
		stateRegistry.triggerEvent(EVENT.ON_BIND)
		return super.onBind(intent)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Timber.d("Service onStartCommand(), ${intent}\n${flags}\n${startId}")
		return super.onStartCommand(intent, flags, startId)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
		Timber.d("Service onGetSession()")
		stateRegistry.updateState(MediaState.INITIALIZED)
		return mediaLibrarySession
	}

	override fun onDestroy() {
		Timber.d("MusicLibraryService onDestroy() is called")
		stateRegistry.triggerEvent(EVENT.ON_DESTROY)
		cancelServiceScope()
		releaseComponent()
		releaseSession()
		super.onDestroy()
	}

	private fun setupNotificationProvider() {
		setMediaNotificationProvider(mediaNotificationProvider)
	}

	private fun setupEventListener() {
		mediaEventListener.start(true)
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
		// NPE in MediaControllerImplBase.java:3041 when calling librarySession.release()
		mediaLibrarySession.release()
	}

	@MainThread
	private fun changeSessionPlayer(player: Player) {
		checkMainThread()
		val get = currentMediaSession.player
		currentMediaSession.player = player
		onPlayerChangedListener.forEach { it.onChanged(get, currentMediaSession.player) }
	}

	private fun startForegroundServiceImpl(id: Int, notification: Notification, isEvent: Boolean) {
		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}

		if (isEvent) stateRegistry.triggerEvent(EVENT.ON_FOREGROUND)
	}

	private fun stopForegroundServiceImpl(id: Int, removeNotification: Boolean, isEvent: Boolean) {
		stopForeground(removeNotification)
		if (removeNotification) {
			val manager = mediaNotificationProvider.notificationManager
			if (manager.activeNotifications.isNotEmpty()) {
				val get = manager.activeNotifications.find { it.id == id }
				if (get != null) {
					Timber.d("stopForeground did not remove Notification, " +
						"\n size: ${manager.activeNotifications.size}" +
						"\n removing id:$id Manually")
					manager.cancel(id)
				}
			}
		}
		if (isEvent) stateRegistry.triggerEvent(EVENT.ON_PAUSE)
	}

	private fun stopServiceImpl(releaseSession: Boolean) {
		stateRegistry.triggerEvent(EVENT.ON_STOP)
		if (releaseSession) this.sessions.forEach { it.release() }
	}

	@MainThread
	fun startForegroundService(id: Int, notification: Notification, ignoreCheck: Boolean = false) {
		checkMainThread()
		if (!ignoreCheck) {
			// shouldn't be called by Notification Validator
			checkState(!LifecycleStateDelegate.isForeground())
		}
		startForegroundServiceImpl(id, notification, !ignoreCheck)
	}

	@MainThread
	fun stopForegroundService(id: Int, removeNotification: Boolean, ignoreCheck: Boolean = false) {
		checkMainThread()
		if (!ignoreCheck) {
			// shouldn't be called by Notification Validator
			checkState(LifecycleStateDelegate.isForeground())
		}
		stopForegroundServiceImpl(id, removeNotification, !ignoreCheck)
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

	inner class SessionCallbackImpl : MediaLibrarySessionCallback {
		override fun onConnect(
			session: MediaSession,
			controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			val result =
				try {
					stateRegistry.updateState(MediaState.CONNECTING)
					super.onConnect(session, controller)
				} catch (e: Exception) {
					stateRegistry.updateState(MediaState.ERROR(e, "onConnect Failed, rejecting..."))
					MediaSession.ConnectionResult.reject()
				}
			return result
		}

		override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
			try {
				stateRegistry.updateState(MediaState.CONNECTED)
				super.onPostConnect(session, controller)
			} catch (e: Exception) {
				stateRegistry.updateState(MediaState.ERROR(e, "onPostConnect Failed"))
			}
		}

		override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
			try {
				stateRegistry.updateState(MediaState.DISCONNECTED)
				super.onDisconnected(session, controller)
			} catch (e: Exception) {
				stateRegistry.updateState(MediaState.ERROR(e, "onDisconnected Failed"))
			}
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

	@MainThread
	inner class StateRegistry : LifecycleOwner {

		private val lifecycleRegistry = LifecycleRegistry(this)

		private var serviceState: STATE = STATE.NOTHING
			set(value) {
				field = value
				LifecycleStateDelegate.updateState(this, field)
				_serviceStateSF.value = field
			}

		private var mediaState: MediaState = MediaState.NOTHING

		@MainThread
		fun triggerEvent(event: EVENT) {
			updateState(event.asServiceState())
		}

		@MainThread
		private fun updateState(state: STATE) {
			checkState(state != serviceState) {
				"State Updated Multiple Times, $state"
			}

			when (state) {
				STATE.NOTHING -> throw IllegalArgumentException()
				STATE.INITIALIZED -> checkState(lifecycleRegistry.currentState == INITIALIZED)
				else -> triggerEvent(state.asLifecycleEvent())
			}

			serviceState = state
		}

		@MainThread
		fun updateState(state: MediaState) {
			checkState(state != serviceState)

			when (state) {
				MediaState.NOTHING -> throw IllegalArgumentException()
				else -> Unit
			}

			mediaState = state
			_mediaStateSF.value = state
		}

		private fun triggerEvent(event: Lifecycle.Event) {
			lifecycleRegistry.handleLifecycleEvent(event)
		}

		override fun getLifecycle(): Lifecycle = lifecycleRegistry
	}

	companion object {
		private var MAX_SESSION = 1

		@JvmStatic
		fun getComponentName(packageName: Context): ComponentName {
			return ComponentName(packageName, MusicLibraryService::class.java)
		}
	}

	sealed class EVENT {
		object ON_CREATE : EVENT()
		object ON_BIND : EVENT()
		object ON_FOREGROUND : EVENT()
		object ON_PAUSE : EVENT()
		object ON_STOP : EVENT()
		object ON_DESTROY : EVENT()

		fun asLifecycleEvent(): Lifecycle.Event {
			return when (this) {
				ON_CREATE -> Lifecycle.Event.ON_CREATE
				ON_BIND -> Lifecycle.Event.ON_START
				ON_FOREGROUND -> Lifecycle.Event.ON_RESUME
				ON_PAUSE -> Lifecycle.Event.ON_PAUSE
				ON_STOP -> Lifecycle.Event.ON_STOP
				ON_DESTROY -> Lifecycle.Event.ON_DESTROY
			}
		}

		fun asServiceState(): STATE {
			return when (this) {
				ON_CREATE -> STATE.CREATED
				ON_BIND -> STATE.BIND
				ON_FOREGROUND -> STATE.FOREGROUND
				ON_PAUSE -> STATE.PAUSED
				ON_STOP -> STATE.STOPPED
				ON_DESTROY -> STATE.DESTROYED
			}
		}
	}

	sealed class STATE {
		object NOTHING : STATE()
		// Init
		object INITIALIZED : STATE()
		// OnCreate pre-Super
		object CREATED : STATE()
		// OnBind pre-Super
		object BIND : STATE()
		// StartForeground
		object FOREGROUND : STATE()
		// StopForeground
		object PAUSED : STATE()
		// StopSelf
		// TODO after Media3 beta Update
		object STOPPED : STATE()
		// OnDestroy pre-super
		object DESTROYED: STATE()

		fun isAtLeast(that: STATE): Boolean = Int.get(this) >= Int.get(that)
		fun isAtMost(that: STATE): Boolean = Int.get(this) <= Int.get(that)
		fun isMoreThan(that: STATE): Boolean = Int.get(this) > Int.get(that)
		fun isLessThan(that: STATE): Boolean = Int.get(this) < Int.get(that)

		fun isForeground(): Boolean = this == FOREGROUND

		fun asLifecycleState(): Lifecycle.State {
			return when(this) {
				NOTHING -> throw IllegalArgumentException()
				INITIALIZED -> Lifecycle.State.INITIALIZED
				CREATED -> Lifecycle.State.CREATED
				BIND -> STARTED
				FOREGROUND -> RESUMED
				PAUSED -> STARTED
				STOPPED -> Lifecycle.State.CREATED
				DESTROYED -> Lifecycle.State.DESTROYED
			}
		}

		fun asLifecycleEvent(): Lifecycle.Event {
			return when(this) {
				NOTHING -> throw IllegalArgumentException()
				INITIALIZED -> throw IllegalArgumentException()
				CREATED -> ON_CREATE
				BIND -> ON_START
				FOREGROUND -> ON_RESUME
				PAUSED -> ON_PAUSE
				STOPPED -> ON_STOP
				DESTROYED -> ON_DESTROY
			}
		}

		private object Int {
			const val Nothing = -1
			const val Destroyed = 0
			const val Initialized = 1
			const val Created = 2
			const val Bind = 3
			const val Foreground = 4
			const val Paused = 3
			const val Stopped = 2

			fun get(state: STATE): kotlin.Int {
				return when (state) {
					NOTHING -> Nothing
					INITIALIZED -> Initialized
					CREATED -> Created
					BIND -> Bind
					FOREGROUND -> Foreground
					PAUSED -> Paused
					STOPPED -> Stopped
					DESTROYED -> Destroyed
				}
			}
		}
	}

	sealed class MediaState {
		object NOTHING : MediaState()
		// OnGetSession
		object INITIALIZED : MediaState()
		// OnConnect
		object CONNECTING : MediaState()
		// OnPostConnect
		object CONNECTED : MediaState()
		// OnDisconnected
		object DISCONNECTED : MediaState()
		// TODO
		data class ERROR (val e: Exception, val msg: String) : MediaState()
	}

	@MainThread
	object LifecycleStateDelegate : ReadOnlyProperty<Any?, STATE> {

		private var currentState: STATE = STATE.NOTHING
		private var currentHashCode: Int? = null

		fun updateState(registry: MusicLibraryService.StateRegistry, state: STATE) {
			when (state) {
				STATE.NOTHING -> throw IllegalArgumentException()
				STATE.INITIALIZED, STATE.CREATED -> currentHashCode = registry.hashCode()
				else -> Unit
			}
			checkState(registry.hashCode() == currentHashCode) {
				"ServiceLifecycleState Failed, currentHash: $currentHashCode, attempt: ${registry.hashCode()}"
			}
			if (currentState != state) {
				currentState = state
				Timber.d("ServiceLifecycleState updated to $state")
			}
		}

		@JvmStatic fun wasLaunched() = currentState != STATE.NOTHING
		@JvmStatic fun isDestroyed() = currentState == STATE.DESTROYED
		@JvmStatic fun isAlive() = currentState.isAtLeast(STATE.INITIALIZED)
		@JvmStatic fun isForeground(): Boolean = currentState.isForeground()

		override fun getValue(thisRef: Any?, property: KProperty<*>): STATE = currentState
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}
}
