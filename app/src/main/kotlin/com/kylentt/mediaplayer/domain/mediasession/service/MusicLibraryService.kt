package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.extenstions.LifecycleEvent
import com.kylentt.mediaplayer.core.extenstions.LifecycleService
import com.kylentt.mediaplayer.core.media3.MediaItemFactory
import com.kylentt.mediaplayer.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MediaNotificationManager
import com.kylentt.mediaplayer.domain.mediasession.service.playback.PlaybackManager
import com.kylentt.mediaplayer.domain.mediasession.service.sessions.SessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.state.MusicLibraryStateManager
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

// Everything in service package is not thread safe

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService(), LifecycleService {

	private val serviceComponents: MutableList<ServiceComponent> = mutableListOf()

	abstract class ServiceComponent {
		private lateinit var impl: ServiceComponent

		protected var componentInteractor: ComponentInteractor? = null
			private set
		protected var serviceInteractor: ServiceInteractor? = null
			private set

		var isStartedInternal: Boolean = false
			private set

		var isReleasedInternal: Boolean = false
			private set

		@CallSuper
		@MainThread
		open fun create(serviceInteractor: ServiceInteractor) {
			checkMainThread()
			checkState(!::impl.isInitialized)
			impl = this
			impl.serviceInteractor = serviceInteractor
		}

		@CallSuper
		@MainThread
		open fun onContextAttached(context: Context) {
			checkMainThread()
		}

		@CallSuper
		@MainThread
		open fun onDependencyInjected(serviceInteractor: ServiceInteractor) {
			checkMainThread()
		}

		@CallSuper
		@MainThread
		open fun start(componentInteractor: ComponentInteractor) {
			checkMainThread()
			checkState(::impl.isInitialized)
			impl.componentInteractor = componentInteractor
			isStartedInternal = true
		}

		@CallSuper
		@MainThread
		open fun stop(componentInteractor: ComponentInteractor) {
			checkMainThread()
			checkState(::impl.isInitialized)
			impl.componentInteractor = null
			isStartedInternal = false
		}

		@CallSuper
		@MainThread
		open fun release(obj: Any) {
			if (isStartedInternal) stop(impl.componentInteractor!!)

			serviceInteractor = null

			checkState(!isStartedInternal
				&& impl.serviceInteractor == null
				&& impl.componentInteractor == null)

			isReleasedInternal = true
		}
	}

	@Inject
	lateinit var coilHelper: CoilHelper

	@Inject
	lateinit var injectedPlayer: ExoPlayer

	@Inject
	lateinit var mediaRepository: MediaRepository

	@Inject
	lateinit var sessionConnector: MediaSessionConnector

	lateinit var notificationManagerService: NotificationManager

	private val stateRegistry = StateRegistry()
	private val sessionCallbackImpl = SessionCallbackImpl()
	private val serviceCommandReceiver = ServiceCommandReceiver()

	private val mediaSessionManager: SessionManager
	private val mediaPlaybackManager: PlaybackManager
	private val mediaNotificationManager: MediaNotificationManager

	private val stateManager: MusicLibraryStateManager

	private val componentInteractor: ComponentInteractor
	private val serviceInteractor = ServiceInteractor()

	private var isReleasing = false
	override val service: Service = this

	private val appDispatchers = AppModule.provideAppDispatchers()
	private val serviceJob = SupervisorJob()

	val serviceStateSF = stateRegistry.serviceStateSF
	val serviceEventSF = stateRegistry.serviceEventSF

	val isServiceStarted
		get() = serviceStateSF.value.isStarted()

	val isServiceForeground
		get() = serviceStateSF.value.isForeground()

	val isServiceStopped
		get() = serviceStateSF.value.isStopped()

	init {
		stateRegistry.onEvent(ServiceEvent.Initialize)

		mediaSessionManager = SessionManager(this, sessionCallbackImpl)
		mediaPlaybackManager = PlaybackManager()
		mediaNotificationManager = MediaNotificationManager()
		stateManager = MusicLibraryStateManager()

		serviceComponents.add(mediaSessionManager)
		serviceComponents.add(mediaPlaybackManager)
		serviceComponents.add(mediaNotificationManager)
		serviceComponents.add(stateManager)

		componentInteractor = ComponentInteractor(
			mediaNotificationManager.Delegate(), mediaSessionManager.Delegate(), stateManager.Delegate()
		)

		serviceComponents.forEach { it.create(serviceInteractor) }
		postInitialize()
	}

	private fun postInitialize() {
		stateRegistry.onEvent(ServiceEvent.PostInitialize)
	}

	override fun onCreate() {
		onContextAttached()
		onPostContextAttached()
		super.onCreate()
		onPostSuperOnCreate()
		onPostCreate()
	}

	private fun onContextAttached() {
		stateRegistry.onEvent(ServiceEvent.ContextAttached)
		serviceComponents.forEach { it.onContextAttached(this) }
		initializeService()
	}

	private fun initializeService() {
		val state = serviceStateSF.value

		checkState(
			state lessThan STATE.Created
				&& state atLeast STATE.ContextAttached
		)

		notificationManagerService = getSystemService(NotificationManager::class.java)
		registerReceiver(serviceCommandReceiver, CommandReceiver.getIntentFilter())
	}

	private fun onPostContextAttached() {
		stateRegistry.onEvent(ServiceEvent.Create)
	}

	private fun onPostSuperOnCreate() {
		stateRegistry.onEvent(ServiceEvent.InjectDependency) // Hilt Injected dependency
		serviceComponents.forEach { it.onDependencyInjected(serviceInteractor) }
		initializeLibrarySession()
	}

	private fun initializeLibrarySession() {
		setMediaNotificationProvider(mediaNotificationManager.getProvider())
	}

	private fun onPostCreate() {
		stateRegistry.onEvent(ServiceEvent.PostCreate)
	}

	override fun onUpdateNotification(session: MediaSession) {
		if (isReleasing) return
		if (isServiceStopped) onStart()
		mediaNotificationManager.onUpdateNotification(session)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (serviceStateSF.value sameAs STATE.Created) {
			onStart()
		}

		Timber.i("MusicLibraryService onStartCommand()," +
			" ${intent}\n${flags}\n${startId}\n${serviceEventSF.value}\n${serviceStateSF.value}")

		stateRegistry.onEvent(ServiceEvent.StartCommand)

		try {
			super.onStartCommand(intent, flags, startId)
		} catch (e: IllegalStateException) {
			Timber.e("super.onStartCommand Failed, \n${e}")
		}

		return START_NOT_STICKY
	}

	private fun onStart() {
		stateRegistry.onEvent(ServiceEvent.Start)

		serviceComponents.forEach {
			if (!it.isStartedInternal) it.start(componentInteractor)
		}

		postStart()
	}

	private fun postStart() {
		stateRegistry.onEvent(ServiceEvent.PostStart)
	}

	override fun onBind(intent: Intent?): IBinder? {
		if (serviceStateSF.value sameAs STATE.Created) {
			onStart()
		}
		stateRegistry.onEvent(ServiceEvent.Binding)
		return super.onBind(intent)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
		return mediaSessionManager.onGetSession(controllerInfo)
	}

	override fun onDestroy() {
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
			notificationManagerService.cancelAll()
			exitProcess(0)
		}
	}

	override fun getLifecycle(): Lifecycle {
		return stateRegistry.lifecycle
	}

	private fun cancelServiceScope() {
		serviceJob.cancel()
	}

	private fun releaseComponent() {
		injectedPlayer.release()
		// maybe Release them by themselves
		serviceComponents.forEach { it.release(this) }
		unregisterReceiver(serviceCommandReceiver)
	}

	private fun releaseSession() {
		checkState(!isServiceForeground) {
			"tried to releaseSession in Foreground State"
		}

		serviceComponents.forEach {
			Timber.i("Service Release session," +
				"\n${it.javaClass.simpleName} released: ${it.isReleasedInternal}")
		}

		serviceComponents.forEach {
			checkState(it.isReleasedInternal) {
				"Components ${it.javaClass.simpleName} Failed to call release,"
			}
		}
		sessionConnector.disconnectService()
	}

	private fun startForegroundService(id: Int, notification: Notification) {
		if (isServiceStopped) onStart()
		startForegroundServiceImpl(id, notification, !isServiceForeground)
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

	private fun stopForegroundService(id: Int, removeNotification: Boolean) {
		stopForegroundServiceImpl(id, removeNotification, isServiceForeground)
	}

	private fun stopForegroundServiceImpl(id: Int, removeNotification: Boolean, isEvent: Boolean) {
		stateRegistry.onEvent(ServiceEvent.Pause)

		stopForeground(removeNotification)

		if (removeNotification) {

			val manager = notificationManagerService
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

	private fun stopService(releaseSession: Boolean) {
		checkState(!isServiceForeground) {
			"Cannot Stop Service on Foreground State"
		}
		stopServiceImpl(releaseSession)
	}

	private fun stopServiceImpl(releaseSession: Boolean) {
		if (releaseSession) {
			isReleasing = true
		}

		stateRegistry.onEvent(ServiceEvent.Stop)

		stopSelf()
		serviceComponents.forEach { it.stop(componentInteractor) }

		postStopService(releaseSession)
	}

	private fun postStopService(releaseSession: Boolean) {
		stateRegistry.onEvent(ServiceEvent.PostStop(releaseSession))
		if (releaseSession) {
			this.sessions.forEach { it.release() }
		}
	}

	private inner class SessionCallbackImpl : MediaLibrarySession.Callback {

		override fun onAddMediaItems(
			mediaSession: MediaSession,
			controller: MediaSession.ControllerInfo,
			mediaItems: MutableList<MediaItem>
		): ListenableFuture<MutableList<MediaItem>> {
			val toReturn = mutableListOf<MediaItem>()

			mediaItems.forEach {
				val uri = it.mediaUri ?: return@forEach
				val item = MediaItemFactory.fillInLocalConfig(it, uri)
				toReturn.add(item)
			}

			return Futures.immediateFuture(toReturn)
		}

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

			Timber.d("MusicLibraryService onEvent: $event")

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

				is ServiceEvent.PostStop -> updateState(STATE.Stopped)

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
					checkState(state upFrom serviceState || state downFrom serviceState) {
						"State jump from $serviceState to $state"
					}
				}
			}

			serviceState = state
		}

		fun updateState(state: MediaState) {}

		private fun onEvent(event: Lifecycle.Event) {
			lifecycleRegistry.handleLifecycleEvent(event)
		}

		override fun getLifecycle(): Lifecycle = lifecycleRegistry
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

		object Create : SingleTimeEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_CREATE
		}

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

		data class PostStop(val releasing: Boolean) : ServiceEvent()

		object Destroy : SingleTimeEvent(), LifecycleEvent {
			override fun asLifecycleEvent(): Event = Lifecycle.Event.ON_DESTROY
		}

		object PostDestroy : SingleTimeEvent()

		companion object {

			@JvmStatic
			fun fromLifecycleEvent(event: Lifecycle.Event): ServiceEvent {
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
		object Destroyed : STATE()

		infix fun atLeast(that: STATE): Boolean = this >= that
		infix fun atMost(that: STATE): Boolean = this <= that
		infix fun lessThan(that: STATE): Boolean = this < that
		infix fun moreThan(that: STATE): Boolean = this > that
		infix fun sameAs(that: STATE): Boolean = this compareTo that == 0

		infix fun downFrom(that: STATE): Boolean =
			ComparableInt.get(this) == (ComparableInt.get(that) - 1)

		infix fun upFrom(that: STATE): Boolean =
			ComparableInt.get(this) == (ComparableInt.get(that) + 1)

		fun isInitialized(): Boolean = this atLeast Initialized
		fun isContextAttached(): Boolean = this atLeast ContextAttached
		fun isDependencyInjected(): Boolean = this atLeast DependencyInjected
		fun isCreated(): Boolean = this atLeast Created
		fun isStarted(): Boolean = this atLeast Started
		fun isForeground(): Boolean = this atLeast Foreground
		fun isStopped(): Boolean = this atMost Stopped
		fun isPaused(): Boolean = this atMost Paused
		fun isDestroyed(): Boolean = this == Destroyed

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
		}
	}

	sealed class MediaState {
		object NOTHING : MediaState()
		object INITIALIZED : MediaState()
		object CONNECTING : MediaState()
		object CONNECTED : MediaState()
		object DISCONNECTED : MediaState()
		data class ERROR(val e: Exception, val msg: String) : MediaState()
	}

	inner class ServiceCommandReceiver : BroadcastReceiver() {

		private fun handleRequestStartService(context: Context) {
			if (isServiceStarted) return
			if (context !== this@MusicLibraryService.baseContext) {
				Timber.w("only MusicLibraryService Component can request start service")
			}
			onStart()
		}

		override fun onReceive(context: Context?, intent: Intent?) {
			Timber.d("ServiceCommandReceiver onReceive $context, $intent")

			if (context == null || intent == null) return

			val action = intent.getStringExtra(CommandReceiver.commandIntentReceiverActionKey) ?: return

			when (action) {
				CommandReceiver.ACTION_CANCEL_ALL_NOTIFICATION -> notificationManagerService.cancelAll()
				CommandReceiver.ACTION_REQUEST_START_SERVICE -> handleRequestStartService(context)
			}
		}
	}

	inner class ComponentInteractor(
		notificationManagerDelegate: MediaNotificationManager.Delegate,
		sessionManagerDelegate: SessionManager.Delegate,
		stateManagerDelegate: MusicLibraryStateManager.Delegate
	) {
		val mediaNotificationManagerDelegator = notificationManagerDelegate
		val mediaSessionManagerDelegator = sessionManagerDelegate
		val stateManagerDelegator = stateManagerDelegate
	}

	inner class ServiceInteractor {

		/**
		 * Must guarantee not null if the service state is between
		 * onContextAttached and onDestroy (onDestroy callback is called right before)
		 *
		 * @return this Service Context, may be null
		 */

		val isForeground
			get() = this@MusicLibraryService.isServiceForeground

		fun getContext(): Context? =
			this@MusicLibraryService.baseContext

		fun getCoroutineMainJob(): Job =
			this@MusicLibraryService.serviceJob

		fun getCoroutineDispatchers(): AppDispatchers =
			this@MusicLibraryService.appDispatchers

		fun getCoilHelper(): CoilHelper =
			this@MusicLibraryService.coilHelper

		fun getSessionConnector(): MediaSessionConnector =
			this@MusicLibraryService.sessionConnector

		fun addLifecycleObserver(observer: LifecycleEventObserver) =
			this@MusicLibraryService.lifecycle.addObserver(observer)

		fun removeLifecycleObserver(observer: LifecycleEventObserver) =
			this@MusicLibraryService.lifecycle.removeObserver(observer)

		fun startForeground(notificationId: Int, notification: Notification) =
			this@MusicLibraryService.startForegroundService(notificationId, notification)

		fun stopForeground(notificationId: Int, removeNotification: Boolean) =
			this@MusicLibraryService.stopForegroundService(notificationId, removeNotification)

		fun stopService(releaseSession: Boolean) =
			this@MusicLibraryService.stopService(releaseSession)
	}

	inner class StateInteractor {
		fun callStart() = onStart()
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

		@JvmStatic
		fun wasLaunched() = currentState != STATE.Nothing

		@JvmStatic
		fun isDestroyed() = currentState == STATE.Destroyed

		@JvmStatic
		fun isAlive() = currentState atLeast STATE.Initialized && !isDestroyed()

		@JvmStatic
		fun isForeground(): Boolean = currentState.isForeground()

		@JvmStatic
		fun isStopped(): Boolean = currentState == STATE.Stopped

		@JvmStatic
		fun isPaused(): Boolean = currentState == STATE.Stopped

		override fun getValue(thisRef: Any?, property: KProperty<*>): STATE = currentState
	}

	object Constants {
		const val SESSION_ID = "FLAMM"
	}

	object CommandReceiver {
		private const val commandIntentReceiverActionName = "ActionName"
		const val commandIntentReceiverActionKey = "ActionKey"

		const val ACTION_CANCEL_ALL_NOTIFICATION = "CANCEL_ALL_NOTIFICATION"
		const val ACTION_REQUEST_START_SERVICE = "REQUEST_START_SERVICE"

		fun getIntentWithAction(): Intent = Intent(commandIntentReceiverActionName)
		fun getIntentFilter(): IntentFilter = IntentFilter(commandIntentReceiverActionName)
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
		fun getComponentName(context: Context? = null): ComponentName {
			return context?.let { ComponentName(it, MusicLibraryService::class.java) }
				?: AppDelegate.componentName(MusicLibraryService::class.java)
		}
	}
}
