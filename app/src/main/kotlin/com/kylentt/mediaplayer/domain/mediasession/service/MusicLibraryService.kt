package com.kylentt.mediaplayer.domain.mediasession.service
import android.app.Notification
import android.app.NotificationManager
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.event.MusicLibraryEventManager
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.util.Objects
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

	private val sessionCallbackImpl = SessionCallbackImpl()

	private val mediaNotificationProvider: MusicLibraryNotificationProvider
	private val mediaSessionManager: MusicLibrarySessionManager
	private val mediaEventManager: MusicLibraryEventManager

	override val service: Service = this

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

	val notificationManager: NotificationManager by lazy {
		getSystemService(NotificationManager::class.java)
	}

	val isServiceForeground
		get() = serviceStateSF.value.isForeground()

	val isDependencyInjected
		get() = ::sessionConnector.isInitialized

	init {
		Timber.i("MusicLibraryService Initializing")

		stateRegistry.onEvent(ServiceEvent.Initialize)

		mediaSessionManager = MusicLibrarySessionManager(musicService = this,
			sessionCallback = sessionCallbackImpl
		)

		mediaNotificationProvider = MusicLibraryNotificationProvider(musicService = this,
			sessionManager = mediaSessionManager
		)

		mediaEventManager = MusicLibraryEventManager(musicService = this,
			sessionManager = mediaSessionManager,
		)

		mediaEventManager.startListener()

		postInitialize()
	}

	private fun postInitialize() {
		stateRegistry.onEvent(ServiceEvent.PostInitialize)

		Timber.i("MusicLibraryService Initialized")
	}

	override fun onCreate() {
		Timber.i("MusicLibraryService onCreate()")

		stateRegistry.onEvent(ServiceEvent.Create)

		super.onCreate()
		mediaSessionManager.initializeSession(this, injectedPlayer)
		setupNotificationProvider()


		onPostCreate()
	}

	private fun onPostCreate() {
		stateRegistry.onEvent(ServiceEvent.PostCreate)

		Timber.i("MusicLibraryService Created")
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (serviceStateSF.value sameAs STATE.CREATED) {
			onStart()
		}

		Timber.i("MusicLibraryService onStartCommand(), ${intent}\n${flags}\n${startId}")
		stateRegistry.onEvent(ServiceEvent.StartCommand)

		try {
			super.onStartCommand(intent, flags, startId)
		} catch (e: IllegalStateException) {
			Timber.e("super.onStartCommand Failed, \n${e}")
		}

		return START_NOT_STICKY
	}

	private fun onStart() {
		Timber.i("MusicLibraryService onStart()")
		stateRegistry.onEvent(ServiceEvent.Start)

		postStart()
	}

	private fun postStart() {
		stateRegistry.onEvent(ServiceEvent.PostStart)

		Timber.i("MusicLibraryService onPostStart()")
	}

	override fun onBind(intent: Intent?): IBinder? {
		if (serviceStateSF.value sameAs STATE.CREATED) {
			onStart()
		}

		Timber.i("MusicLibraryService onBind()")
		stateRegistry.onEvent(ServiceEvent.Binding)

		return super.onBind(intent)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
		Timber.i("MusicLibraryService onGetSession()")
		return mediaSessionManager.onGetSession(controllerInfo)
	}

	override fun onDestroy() {
		Timber.i("MusicLibraryService onDestroy()")
		stateRegistry.onEvent(ServiceEvent.Destroy)
		cancelServiceScope()
		releaseComponent()
		releaseSession()

		super.onDestroy()
		onPostDestroy()
	}

	private fun onPostDestroy() {
		stateRegistry.onEvent(ServiceEvent.PostDestroy)

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
		stateRegistry.onEvent(ServiceEvent.StartForeground)

		if (VersionHelper.hasQ()) {
			startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(id, notification)
		}

		postStartForegroundService(isEvent)
	}

	private fun postStartForegroundService(isEvent: Boolean) {
		if (isEvent) stateRegistry.onEvent(ServiceEvent.PostForeground)
	}

	private fun stopForegroundServiceImpl(id: Int, removeNotification: Boolean, isEvent: Boolean) {
		stateRegistry.onEvent(ServiceEvent.Pause)

		stopForeground(removeNotification)

		if (removeNotification) {

			val manager = notificationManager
			if (manager.activeNotifications.isNotEmpty()) {

				val get = manager.activeNotifications.find { it.id == id }
				if (get != null) {
					manager.cancel(id)
				}
			}
		}

		postStopForegroundService(isEvent)
	}

	private fun postStopForegroundService(isEvent: Boolean) {
		if (isEvent) stateRegistry.onEvent(ServiceEvent.PostPause)
	}

	private fun stopServiceImpl(releaseSession: Boolean) {
		stateRegistry.onEvent(ServiceEvent.Stop)
		stopSelf()

		postStopService(releaseSession)
	}

	private fun postStopService(releaseSession: Boolean) {
		stateRegistry.onEvent(ServiceEvent.PostStop)
		if (releaseSession) {
			this.sessions.forEach { it.release() }
		}
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

	/**
	 * Manage [MusicLibraryService] State and Events
	 *
	 * Observable [LifecycleOwner] and Collectable [StateFlow] of State and Events
	 */

	private inner class StateRegistry : LifecycleOwner {

		private val lifecycleRegistry = LifecycleRegistry(this)

		private val _serviceStateSF = MutableStateFlow<STATE>(STATE.NOTHING)
		private val _serviceEventSF = MutableStateFlow<ServiceEvent>(ServiceEvent.Initialize)

		val serviceStateSF = _serviceStateSF.asStateFlow()
		val serviceEventSF = _serviceEventSF.asStateFlow()

		private var serviceState: STATE = STATE.NOTHING
			set(value) {
				field = value
				LifecycleStateDelegate.updateState(this, field)
				_serviceStateSF.value = field
			}

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
				ServiceEvent.Initialize -> Unit

				ServiceEvent.PostInitialize -> updateState(STATE.INITIALIZED)

				ServiceEvent.Create -> Unit

				ServiceEvent.PostCreate -> updateState(STATE.CREATED)

				ServiceEvent.Start -> Unit

				ServiceEvent.PostStart -> updateState(STATE.STARTED)

				ServiceEvent.StartCommand -> Unit

				ServiceEvent.Binding -> Unit

				ServiceEvent.StartForeground -> Unit

				ServiceEvent.PostForeground -> updateState(STATE.FOREGROUND)

				ServiceEvent.Pause -> Unit

				ServiceEvent.PostPause -> updateState(STATE.PAUSED)

				ServiceEvent.Stop -> Unit

				ServiceEvent.PostStop -> updateState(STATE.STOPPED)

				ServiceEvent.Destroy -> Unit

				ServiceEvent.PostDestroy -> updateState(STATE.DESTROYED)
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

		fun updateState(state: MediaState) { }

		private fun onEvent(event: Lifecycle.Event) {
			lifecycleRegistry.handleLifecycleEvent(event)
		}

		override fun getLifecycle(): Lifecycle = lifecycleRegistry
	}

	sealed class ServiceEvent {

		object Initialize : ServiceEvent()

		object PostInitialize : ServiceEvent()

		object Create : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_CREATE
		}

		object PostCreate : ServiceEvent()

		object Start : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_START
		}

		object PostStart : ServiceEvent()

		object StartCommand : ServiceEvent()

		object Binding : ServiceEvent()

		object StartForeground : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_RESUME
		}

		object PostForeground : ServiceEvent()

		object Pause : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_PAUSE
		}

		object PostPause : ServiceEvent()

		object Stop : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_STOP
		}

		object PostStop : ServiceEvent()

		object Destroy : ServiceEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_DESTROY
		}

		object PostDestroy : ServiceEvent()

		companion object {
			@JvmStatic fun fromLifecycleEvent(event: Lifecycle.Event): ServiceEvent {
				return when (event) {
					Lifecycle.Event.ON_CREATE -> Create
					Lifecycle.Event.ON_START -> StartCommand
					Lifecycle.Event.ON_RESUME -> StartForeground
					Lifecycle.Event.ON_PAUSE -> Pause
					Lifecycle.Event.ON_STOP -> Stop
					Lifecycle.Event.ON_DESTROY -> Destroy
					Lifecycle.Event.ON_ANY -> TODO()
				}
			}
		}
	}

	sealed class STATE {

		object NOTHING : STATE()

		object INITIALIZED : STATE()

		object CREATED : STATE()

		object STARTED : STATE()

		object FOREGROUND : STATE()

		object PAUSED : STATE()

		object STOPPED : STATE()

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

			fun get(state: STATE): Int {
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
