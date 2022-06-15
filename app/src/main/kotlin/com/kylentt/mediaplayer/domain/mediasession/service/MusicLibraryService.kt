package com.kylentt.mediaplayer.domain.mediasession.service
import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback
import androidx.media3.session.MediaSession
import androidx.recyclerview.widget.RecyclerView
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryEventManager
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

fun interface OnChanged <T> {
	fun onChanged(old: T?, new: T)
}

interface LifecycleEvent {
	fun asLifecycleEvent(): Lifecycle.Event
}

interface LifecycleService : LifecycleOwner {
	val service: Service
}

// Everything in service package is not thread safe

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService(), LifecycleService {

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	/** @see [StateRegistry] */

	private val stateRegistry = StateRegistry()

	private val mediaItemFillerImpl = MediaItemFillerImpl()
	private val sessionCallbackImpl = SessionCallbackImpl()

	private val mediaNotificationProvider: MusicLibraryNotificationProvider
	private val mediaSessionManager: MusicLibrarySessionManager
	private val mediaEventManager: MusicLibraryEventManager

	val coroutineDispatchers = AppModule.provideAppDispatchers()

	val serviceJob = SupervisorJob()

	val mainImmediateScope =
		CoroutineScope(coroutineDispatchers.mainImmediate + serviceJob)

	val mainScope =
		CoroutineScope(coroutineDispatchers.main + serviceJob)

	val ioScope =
		CoroutineScope(coroutineDispatchers.io + serviceJob)

	val serviceStateSF = stateRegistry.serviceStateSF
	val serviceEventSF = stateRegistry.serviceEventSF
	val mediaStateSF = stateRegistry.mediaStateSF

	override val service: Service = this

	val isServiceForeground
		get() = serviceStateSF.value.isForeground()

	val isDependencyInjected
		get() = ::sessionConnector.isInitialized

	init {
		stateRegistry.onEvent(ServiceEvent.ON_INITIALIZE)

		mediaSessionManager = MusicLibrarySessionManager(musicService = this,
			mediaItemFiller = mediaItemFillerImpl,
			sessionCallback = sessionCallbackImpl
		)

		mediaNotificationProvider = MusicLibraryNotificationProvider(
			sessionManager = mediaSessionManager,
			musicService = this
		)

		mediaEventManager = MusicLibraryEventManager(musicService = this,
			sessionManager = mediaSessionManager,
			notificationProvider = mediaNotificationProvider
		)

		mediaEventManager.startListener()
	}

	override fun onCreate() {
		Timber.i("Service onCreate()")

		stateRegistry.onEvent(ServiceEvent.ON_CREATE)
		super.onCreate()

		setupNotificationProvider()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Timber.i("Service onStartCommand(), ${intent}\n${flags}\n${startId}")

		stateRegistry.onEvent(ServiceEvent.ON_START_COMMAND)

		try {
			super.onStartCommand(intent, flags, startId)
		} catch (e: IllegalStateException) {
			Timber.e("onStartCommand Failed, \n${e}")
		}

		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder? {
		Timber.i("Service onBind()")
		stateRegistry.onEvent(ServiceEvent.ON_BIND)
		return super.onBind(intent)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
		Timber.i("Service onGetSession()")
		stateRegistry.updateState(MediaState.INITIALIZED)
		return mediaSessionManager.onGetSession(controllerInfo)
	}

	override fun onDestroy() {
		Timber.i("MusicLibraryService onDestroy() is called")
		stateRegistry.onEvent(ServiceEvent.ON_DESTROY)
		cancelServiceScope()
		releaseComponent()
		releaseSession()

		super.onDestroy()

		if (!MainActivity.isAlive) {
			// could Leak
			// TODO: CleanUp
			exitProcess(0)
		}
	}

	override fun getLifecycle(): Lifecycle {
		return stateRegistry.lifecycle
	}

	private fun setupNotificationProvider() {
		setMediaNotificationProvider(mediaNotificationProvider)
	}

	private fun cancelServiceScope() {
		serviceJob.cancel()

		checkState(!ioScope.isActive && !mainScope.isActive && !mainImmediateScope.isActive)
	}

	private fun releaseComponent() {
		injectedPlayer.release()
		mediaSessionManager.release(this)
	}

	private fun releaseSession() {
		checkState(!isServiceForeground) {
			"tried to releaseSession in Foreground State"
		}
		sessionConnector.disconnectService()
	}

	private fun startForegroundServiceImpl(id: Int, notification: Notification, isEvent: Boolean) {
		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}

		if (isEvent) stateRegistry.onEvent(ServiceEvent.ON_FOREGROUND)
	}

	private fun stopForegroundServiceImpl(id: Int, removeNotification: Boolean, isEvent: Boolean) {
		stopForeground(removeNotification)

		if (removeNotification) {

			val manager = mediaNotificationProvider.notificationManager
			if (manager.activeNotifications.isNotEmpty()) {

				val get = manager.activeNotifications.find { it.id == id }
				if (get != null) {
					manager.cancel(id)
				}
			}
		}
		if (isEvent) stateRegistry.onEvent(ServiceEvent.ON_PAUSE)
	}

	private fun stopServiceImpl(releaseSession: Boolean) {
		stateRegistry.onEvent(ServiceEvent.ON_STOP)
		stopSelf()
		if (releaseSession) this.sessions.forEach { it.release() }
	}

	fun startForegroundService(id: Int, notification: Notification, isEvent: Boolean = false) {
		Timber.d("startForegroundService, isEvent: $isEvent")

		if (isEvent) {
			checkState(!serviceStateSF.value.isForeground())
		}
		startForegroundServiceImpl(id, notification, isEvent)
	}

	fun stopForegroundService(id: Int, removeNotification: Boolean, isEvent: Boolean = false) {
		Timber.d("stopForegroundService, isEvent: $isEvent")

		if (isEvent) {
			checkState(serviceStateSF.value.isForeground())
		}
		stopForegroundServiceImpl(id, removeNotification, isEvent)
	}

	fun updateForegroundState(isForeground: Boolean) {
		if (isForeground) {
			stateRegistry.setState(STATE.FOREGROUND)
		} else {
			stateRegistry.setState(STATE.PAUSED)
		}
	}

	fun stopService(releaseSession: Boolean) {
		if (isServiceForeground) {
			stopForegroundService(mediaNotificationProvider.mediaNotificationId,
				removeNotification = true, isEvent = true
			)
		}
		stopServiceImpl(releaseSession)
	}

	private inner class SessionCallbackImpl : MediaLibrarySessionCallback {
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

	/**
	 * Manage [MusicLibraryService] State and Events
	 *
	 * Observable [LifecycleOwner] and Collectable [StateFlow] of State and Events
	 */

	private inner class StateRegistry : LifecycleOwner {

		private val lifecycleRegistry = LifecycleRegistry(this)

		private val _serviceEventSF = MutableStateFlow<ServiceEvent>(ServiceEvent.ON_INITIALIZE)
		private val _serviceStateSF = MutableStateFlow<STATE>(STATE.NOTHING)
		private val _mediaStateSF = MutableStateFlow<MediaState>(MediaState.NOTHING)

		private var serviceState: STATE = STATE.NOTHING
			set(value) {
				field = value
				LifecycleStateDelegate.updateState(this, field)
				_serviceStateSF.value = field
			}

		private var mediaState: MediaState = MediaState.NOTHING
			set(value) {
				field = value
				_mediaStateSF.value = field
			}

		val serviceEventSF =  _serviceEventSF.asStateFlow()
		val serviceStateSF = _serviceStateSF.asStateFlow()
		val mediaStateSF = _mediaStateSF.asStateFlow()

		fun setState(state: STATE) {
			updateState(state)
		}

		fun onEvent(event: ServiceEvent) {

			if (event is LifecycleEvent) {
				onEvent(event.asLifecycleEvent())
			}

			_serviceEventSF.value = event
			updateState(event)
		}

		private fun updateState(event: ServiceEvent) {
			when (event) {
				ServiceEvent.ON_INITIALIZE -> updateState(STATE.INITIALIZED)

				ServiceEvent.ON_CREATE -> updateState(STATE.CREATED)

				ServiceEvent.ON_START_COMMAND -> {
					if (serviceState sameAs STATE.CREATED) updateState(STATE.STARTED)
				}

				ServiceEvent.ON_BIND -> updateState(STATE.STARTED)

				ServiceEvent.ON_FOREGROUND -> updateState(STATE.FOREGROUND)

				ServiceEvent.ON_PAUSE -> updateState(STATE.PAUSED)

				ServiceEvent.ON_STOP -> updateState(STATE.STOPPED)

				ServiceEvent.ON_DESTROY -> updateState(STATE.DESTROYED)
			}
		}

		private fun updateState(state: STATE) {
			checkState(state != serviceState) {
				"State Updated Multiple Times, $state"
			}

			when (state) {
				STATE.NOTHING -> throw IllegalArgumentException()
				STATE.INITIALIZED -> checkState(lifecycleRegistry.currentState == INITIALIZED)
				else -> {
					checkState( state upFrom serviceState  || state downFrom serviceState) {
						"State jump from $serviceState to $state"
					}
				}
			}

			serviceState = state
		}

		fun updateState(state: MediaState) {
			checkState(state != serviceState)

			when (state) {
				MediaState.NOTHING -> throw IllegalArgumentException()
				else -> Unit
			}

			mediaState = state
			_mediaStateSF.value = state
		}

		private fun onEvent(event: Lifecycle.Event) {
			lifecycleRegistry.handleLifecycleEvent(event)
		}

		override fun getLifecycle(): Lifecycle = lifecycleRegistry
	}

	sealed class ServiceEvent {

		object ON_INITIALIZE : ServiceEvent()

		object ON_CREATE : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_CREATE
		}

		object ON_START_COMMAND : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_START
		}

		object ON_BIND : ServiceEvent()

		object ON_FOREGROUND : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_START
		}

		object ON_PAUSE : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_PAUSE
		}

		object ON_STOP : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_STOP
		}

		object ON_DESTROY : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_DESTROY
		}

		companion object {
			@JvmStatic fun fromLifecycleEvent(event: Lifecycle.Event): ServiceEvent {
				return when (event) {
					Lifecycle.Event.ON_CREATE -> ON_CREATE
					Lifecycle.Event.ON_START -> ON_START_COMMAND
					Lifecycle.Event.ON_RESUME -> ON_FOREGROUND
					Lifecycle.Event.ON_PAUSE -> ON_PAUSE
					Lifecycle.Event.ON_STOP -> ON_STOP
					Lifecycle.Event.ON_DESTROY -> ON_DESTROY
					Lifecycle.Event.ON_ANY -> TODO()
				}
			}
		}
	}

	sealed class STATE {

		object NOTHING : STATE()
		// Init
		object INITIALIZED : STATE()
		// OnCreate pre-Super
		object CREATED : STATE()
		object STARTED : STATE()
		// StartForeground
		object FOREGROUND : STATE()
		// StopForeground
		object PAUSED : STATE()
		// StopSelf
		// TODO after Media3 beta Update
		object STOPPED : STATE()
		// OnDestroy pre-super
		object DESTROYED: STATE()

		infix fun atLeast(that: STATE): Boolean = INT.get(this) >= INT.get(that)
		infix fun atMost(that: STATE): Boolean = INT.get(this) <= INT.get(that)
		infix fun lessThan(that: STATE): Boolean = INT.get(this) < INT.get(that)
		infix fun moreThan(that: STATE): Boolean = INT.get(this) > INT.get(that)
		infix fun sameAs(that: STATE): Boolean = INT.get(this) == INT.get(that)

		infix fun downFrom(that: STATE): Boolean = INT.get(this) == (INT.get(that) - 1)
		infix fun upFrom(that: STATE): Boolean = INT.get(this) == (INT.get(that) + 1)

		fun isForeground(): Boolean = this == FOREGROUND

		private object INT {
			const val Nothing = -1
			const val Initialized = 0
			const val Created = 1
			const val Started = 2
			const val Foreground = 3
			const val Paused = 2
			const val Stopped = 1
			const val Destroyed = 0

			fun get(state: STATE): kotlin.Int {
				return when (state) {
					NOTHING -> Nothing
					INITIALIZED -> Initialized
					CREATED -> Created
					STARTED -> Started
					FOREGROUND -> Foreground
					PAUSED -> Paused
					STOPPED -> Stopped
					DESTROYED -> Destroyed
				}
			}
		}

		companion object {
			fun fromLifecycleState(state: Lifecycle.State): STATE {
				return when (state) {
					Lifecycle.State.INITIALIZED -> INITIALIZED
					Lifecycle.State.CREATED -> CREATED
					Lifecycle.State.STARTED -> STARTED
					Lifecycle.State.RESUMED -> FOREGROUND
					Lifecycle.State.DESTROYED -> DESTROYED
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

	object LifecycleStateDelegate : ReadOnlyProperty<Any?, STATE> {

		private var currentState: STATE = STATE.NOTHING
		private var currentHashCode: Int? = null

		fun updateState(holder: Any, state: STATE) {

			when (state) {
				STATE.NOTHING -> throw IllegalArgumentException()
				STATE.INITIALIZED -> currentHashCode = holder.hashCode()
				else -> {
					checkState(holder.hashCode() == currentHashCode) {
						"ServiceLifecycleState Failed," +
							"\ncurrentHash: $currentHashCode, attempt: ${holder.hashCode()}"
					}
					checkState(state != currentState) {
						"ServiceLifecycleState Failed," +
							"\ncurrentState: $currentState, attempt: $state"
					}
				}
			}
			currentState = state
			Timber.d("ServiceLifecycleState updated to $state")
		}

		@JvmStatic fun wasLaunched() = currentState != STATE.NOTHING
		@JvmStatic fun isDestroyed() = currentState == STATE.DESTROYED
		@JvmStatic fun isAlive() = currentState atLeast STATE.INITIALIZED
		@JvmStatic fun isForeground(): Boolean = currentState.isForeground()

		override fun getValue(thisRef: Any?, property: KProperty<*>): STATE = currentState
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}

	companion object {

		/**
		 * current MAX number of MediaSession
		 *
		 * [MusicLibraryService] considered / tested in its current implementation
		 */

		const val MAX_SESSION = 1

		/**
		 * @return [ComponentName] of [MusicLibraryService]
		 */

		@JvmStatic
		fun getComponentName(): ComponentName {
			return AppDelegate.componentName(MusicLibraryService::class.java)
		}
	}
}
