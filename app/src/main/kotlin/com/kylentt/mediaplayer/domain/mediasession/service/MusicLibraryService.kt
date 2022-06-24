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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.domain.mediasession.service.playback.MusicLibraryPlaybackManager
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.MusicLibrarySessionManager
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	lateinit var notificationManager: NotificationManager

	private val stateRegistry = StateRegistry()
	private val sessionCallbackImpl = SessionCallbackImpl()

	private val mediaNotificationProvider: MusicLibraryNotificationProvider
	private val mediaSessionManager: MusicLibrarySessionManager
	private val mediaPlaybackManager: MusicLibraryPlaybackManager

	override val service: Service = this

	val coroutineDispatchers = AppModule.provideAppDispatchers()
	val serviceJob = SupervisorJob()

	val mainImmediateScope =
		CoroutineScope(coroutineDispatchers.mainImmediate + serviceJob)

	val mainScope =
		CoroutineScope(coroutineDispatchers.main + serviceJob)

	val workerScope =
		CoroutineScope(coroutineDispatchers.io + serviceJob)

	val serviceStateSF = stateRegistry.serviceStateSF
	val serviceEventSF = stateRegistry.serviceEventSF

	val isServiceForeground
		get() = serviceStateSF.value.isForeground()

	init {
		Timber.i("MusicLibraryService Initializing")

		stateRegistry.onEvent(ServiceEvent.Initialize)

		mediaSessionManager = MusicLibrarySessionManager(musicLibrary = this,
			sessionCallback = sessionCallbackImpl
		)

		mediaNotificationProvider = MusicLibraryNotificationProvider(musicLibrary = this,
			sessionManager = mediaSessionManager
		)

		mediaPlaybackManager = MusicLibraryPlaybackManager(musicLibrary = this,
			sessionManager = mediaSessionManager
		)

		postInitialize()
	}

	private fun postInitialize() {
		stateRegistry.onEvent(ServiceEvent.PostInitialize)

		Timber.i("MusicLibraryService Initialized")
	}

	override fun onCreate() {
		Timber.i("MusicLibraryService onCreate()")

		checkState(this.baseContext != null)
		stateRegistry.onEvent(ServiceEvent.ContextAttached)
		initializeService()

		stateRegistry.onEvent(ServiceEvent.Create)
		super.onCreate()

		stateRegistry.onEvent(ServiceEvent.InjectDependency) // Hilt Injected dependency
		mediaSessionManager.initializeSession(this, injectedPlayer)
		setupNotificationProvider()

		onPostCreate()
	}

	private fun initializeService() {
		val state = serviceStateSF.value

		checkState(state lessThan STATE.Created
			&& state atLeast STATE.ContextAttached
		)

		notificationManager = getSystemService(NotificationManager::class.java)
	}

	private fun onPostCreate() {
		stateRegistry.onEvent(ServiceEvent.PostCreate)

		Timber.i("MusicLibraryService Created")
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (serviceStateSF.value sameAs STATE.Created) {
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
		if (serviceStateSF.value sameAs STATE.Created) {
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
		checkState(!mainImmediateScope.isActive && !mainScope.isActive && !workerScope.isActive)
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
			stateRegistry.setState(STATE.Foreground)
		} else {
			stateRegistry.setState(STATE.Paused)
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

		private val _serviceStateSF = MutableStateFlow<STATE>(STATE.Nothing)
		private val _serviceEventSF = MutableStateFlow<ServiceEvent>(ServiceEvent.Initialize)

		val serviceStateSF = _serviceStateSF.asStateFlow()
		val serviceEventSF = _serviceEventSF.asStateFlow()

		private var serviceState: STATE = STATE.Nothing
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

			if (event is ServiceEvent.SingleTimeEvent) {
				checkState(!event.isDispatched) {
					"$event is a SingleTimeEvent but called multiple Times"
				}
				event.consume()
			}

			_serviceEventSF.value = event
			updateState(event)
		}

		private fun updateState(event: ServiceEvent) {
			when (event) {
				ServiceEvent.Initialize -> Unit

				ServiceEvent.PostInitialize -> updateState(STATE.Initialized)

				ServiceEvent.ContextAttached -> updateState(STATE.ContextAttached)

				ServiceEvent.Create -> Unit

				ServiceEvent.InjectDependency -> updateState(STATE.DependencyInjected)

				ServiceEvent.PostCreate -> updateState(STATE.Created)

				ServiceEvent.Start -> Unit

				ServiceEvent.PostStart -> updateState(STATE.Started)

				ServiceEvent.StartCommand -> Unit

				ServiceEvent.Binding -> Unit

				ServiceEvent.StartForeground -> Unit

				ServiceEvent.PostForeground -> updateState(STATE.Foreground)

				ServiceEvent.Pause -> Unit

				ServiceEvent.PostPause -> updateState(STATE.Paused)

				ServiceEvent.Stop -> Unit

				ServiceEvent.PostStop -> updateState(STATE.Stopped)

				ServiceEvent.Destroy -> Unit

				ServiceEvent.PostDestroy -> updateState(STATE.Destroyed)

				is ServiceEvent.SingleTimeEvent -> throw NotImplementedError()
			}
		}

		private fun updateState(state: STATE) {
			checkState(state != serviceState) {
				"State Updated Multiple Times, $state"
			}

			when (state) {
				STATE.Nothing -> throw IllegalArgumentException()
				STATE.Initialized -> checkState(lifecycleRegistry.currentState == INITIALIZED)
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

		// end of StateRegistry
	}

	sealed class ServiceEvent {

		sealed class SingleTimeEvent : ServiceEvent() {
			private var consumed = false

			val isDispatched
				get() = consumed

			fun consume() {
				consumed = true
			}
		}

		object Initialize : SingleTimeEvent()
		object PostInitialize : SingleTimeEvent()
		object ContextAttached : SingleTimeEvent()
		object Create : SingleTimeEvent()
		object InjectDependency : SingleTimeEvent()
		object PostCreate : SingleTimeEvent()

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

		object Destroy : SingleTimeEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_DESTROY
		}

		object PostDestroy : SingleTimeEvent()

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
			// end of ServiceEvent.Companion
		}
		// end of ServiceEvent
	}

	sealed class STATE : Comparable<STATE> {

		override fun compareTo(other: STATE): Int = when {
			ComparableInt.get(this) > ComparableInt.get(other) -> 1
			ComparableInt.get(this) < ComparableInt.get(other) -> -1
			else -> 0
		}

		object Nothing : STATE()

		object Initialized : STATE()

		object ContextAttached : STATE()

		object DependencyInjected : STATE()

		object Created : STATE()

		object Started : STATE()

		object Foreground : STATE()

		object Paused : STATE()

		object Stopped : STATE()

		object Destroyed: STATE()

		infix fun atLeast(that: STATE): Boolean = this >= that
		infix fun atMost(that: STATE): Boolean = this <= that
		infix fun lessThan(that: STATE): Boolean = this < that
		infix fun moreThan(that: STATE): Boolean = this > that
		infix fun sameAs(that: STATE): Boolean = this == that

		infix fun downFrom(that: STATE): Boolean = ComparableInt.get(this) == (ComparableInt.get(that) - 1)
		infix fun upFrom(that: STATE): Boolean = ComparableInt.get(this) == (ComparableInt.get(that) + 1)

		fun isForeground(): Boolean = this == Foreground

		private object ComparableInt {
			const val NothingInt = -1
			const val InitializedInt = 0
			const val ContextAttachedInt = 1
			const val DependencyInjectedInt = 2
			const val CreatedInt = 3
			const val StartedInt = 4
			const val ForegroundInt = 5
			const val PausedInt = 4
			const val StoppedInt = 3
			const val DestroyedInt = 2

			fun get(state: STATE): Int {
				return when (state) {
					Nothing -> NothingInt
					Initialized -> InitializedInt
					ContextAttached -> ContextAttachedInt
					DependencyInjected -> DependencyInjectedInt
					Created -> CreatedInt
					Started -> StartedInt
					Foreground -> ForegroundInt
					Paused -> PausedInt
					Stopped -> StoppedInt
					Destroyed -> DestroyedInt
				}
			}
			// end of ComparableInt
		}

		companion object {
			fun fromLifecycleState(state: Lifecycle.State): STATE {
				return when (state) {
					Lifecycle.State.INITIALIZED -> Initialized
					Lifecycle.State.CREATED -> Created
					Lifecycle.State.STARTED -> Started
					Lifecycle.State.RESUMED -> Foreground
					Lifecycle.State.DESTROYED -> Destroyed
				}
			}
			// end of STATE.Companion
		}
		// end of STATE
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

		// end of MediaState
	}

	object LifecycleStateDelegate : ReadOnlyProperty<Any?, STATE> {

		private var currentState: STATE = STATE.Nothing
		private var currentHashCode: Int? = null

		fun updateState(holder: Any, state: STATE) {

			when (state) {
				STATE.Nothing -> throw IllegalArgumentException()
				STATE.Initialized -> currentHashCode = holder.hashCode()
				else -> {
					checkState(holder.hashCode() == currentHashCode) {
						"ServiceLifecycleState Failed," +
							"\ncurrentHash: $currentHashCode, attempt: ${holder.hashCode()}"
					}
					checkState(state != currentState) {
						"ServiceLifecycleState Failed, " + "State Updated Multiple Times"
							"\ncurrentState: $currentState, attempt: $state"
					}
				}
			}
			currentState = state
			Timber.d("ServiceLifecycleState updated to $state")
		}

		@JvmStatic fun wasLaunched() = currentState != STATE.Nothing
		@JvmStatic fun isDestroyed() = currentState == STATE.Destroyed
		@JvmStatic fun isAlive() = currentState atLeast STATE.Initialized
		@JvmStatic fun isForeground(): Boolean = currentState.isForeground()

		override fun getValue(thisRef: Any?, property: KProperty<*>): STATE = currentState

		// end of LifecycleStateDelegate
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}

	companion object {

		/**
		 * current MAX number of MediaLibrarySession
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

		// end of MusicLibraryService.Companion
	}
}
