package com.kylentt.mediaplayer.domain.mediasession.libraryservice

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.core.extenstions.forEachClear
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionConnector
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.notification.MediaNotificationManager
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.playback.PlaybackManager
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.sessions.SessionManager
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.sessions.SessionProvider
import com.kylentt.mediaplayer.domain.mediasession.libraryservice.state.StateManager
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService() {

	private val serviceBroadcastReceivers: MutableSet<BroadcastReceiver> = mutableSetOf()
	private val serviceComponents: MutableSet<ServiceComponent> = mutableSetOf()
	private val serviceJob = SupervisorJob()
	private val stateRegistry = StateRegistry()

	@Inject lateinit var notificationManagerService: NotificationManager
	@Inject lateinit var injectedExoPlayer: ExoPlayer
	@Inject lateinit var injectedSessionConnector: MediaSessionConnector

	private val isServiceCreated
		get() = stateRegistry.serviceStateSF.value >= ServiceState.Created
	private val isServiceStarted
	  get() = stateRegistry.serviceStateSF.value >= ServiceState.Started
	private val isServiceForeground
		get() = stateRegistry.serviceStateSF.value == ServiceState.Foreground
	private val isServicePaused
		get() = stateRegistry.serviceStateSF.value >= ServiceState.Paused
	private val isServiceStopped
		get() = stateRegistry.serviceStateSF.value <= ServiceState.Stopped
	private val isServiceReleased
		get() = stateRegistry.serviceStateSF.value <= ServiceState.Released
	private val isServiceDestroyed
		get() = stateRegistry.serviceStateSF.value <= ServiceState.Destroyed

	private var mReleasing = false

	private val sessionManager = SessionManager(SessionProvider(this))
	private val stateManager = StateManager(StateInteractor())
	private val notificationManager = MediaNotificationManager(MediaNotificationId)
	private val playbackManager = PlaybackManager()

	init {
		serviceComponents.add(sessionManager)
		serviceComponents.add(stateManager)
		serviceComponents.add(playbackManager)
		serviceComponents.add(notificationManager)
		onPostInitialize()
	}

	private fun onPostInitialize() {
		stateRegistry.onEvent(ServiceEvent.Initialize, true)
		serviceComponents.forEach { it.createComponent(this) }
	}

	override fun onCreate() {
		onContextAttached()
		super.onCreate()
		onDependencyInjected()
		postCreate()
	}

	private fun onContextAttached() {
		stateRegistry.onEvent(ServiceEvent.AttachContext, true)
		serviceComponents.forEach { it.notifyContextAttached(this) }
	}

	private fun onDependencyInjected() {
		stateRegistry.onEvent(ServiceEvent.InjectDependency, true)
		serviceComponents.forEach { it.notifyDependencyInjected() }
	}

	private fun postCreate() {
		stateRegistry.onEvent(ServiceEvent.Create, true)
	}

	private fun onStart() {
		stateRegistry.onEvent(ServiceEvent.Start, true)
		serviceComponents.forEach { it.startComponent(this) }
	}

	override fun onBind(intent: Intent?): IBinder? {
		if (!isServiceStarted) onStart()
		return super.onBind(intent)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (!isServiceStarted) onStart()
		return super.onStartCommand(intent, flags, startId)
	}

	private fun startForegroundService(notification: Notification) {
		if (!isServiceStarted) onStart()
		if (VersionHelper.hasQ()) {
			startForeground(MediaNotificationId, notification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
		} else {
			startForeground(MediaNotificationId, notification)
		}
		if (!isServiceForeground) {
			stateRegistry.onEvent(ServiceEvent.ResumeForeground, true)
		}
	}

	private fun stopForegroundService(removeNotification: Boolean) {
		stopForeground(removeNotification)
		if (removeNotification) notificationManagerService.cancel(MediaNotificationId)
		if (isServiceForeground) {
			stateRegistry.onEvent(ServiceEvent.PauseForeground, true)
		}
	}

	private fun stopService(release: Boolean) {
		if (release) mReleasing = true
		if (isServiceForeground) stopForeground(release)
		serviceComponents.forEach { if (it is ServiceComponent.Stoppable) it.stopComponent(release) }
		stopSelf()

		stateRegistry.onEvent(ServiceEvent.Stop(release), true)
		if (release) releaseService()
	}

	private fun releaseService() {
		if (!isServiceStopped) stopService(true)
		releaseComponent()
		releaseSessions()

		checkState(!serviceJob.isActive && sessions.isEmpty()
			&& serviceBroadcastReceivers.isEmpty() && serviceComponents.isEmpty()
		)

		stateRegistry.onEvent(ServiceEvent.Release, true)
	}

	private fun releaseComponent() {
		serviceJob.cancel()
		injectedExoPlayer.release()
		serviceComponents.forEachClear { it.releaseComponent(this) }
		serviceBroadcastReceivers.forEachClear { unregisterReceiver(it) }
	}

	private fun releaseSessions() {
		injectedSessionConnector.disconnectService()
		sessions.forEachClear { it.release() }
	}

	override fun onDestroy() {
		if (!isServiceReleased) releaseService()
		super.onDestroy()
		postDestroy()
	}

	private fun postDestroy() {
		checkState(serviceComponents.isEmpty() && serviceBroadcastReceivers.isEmpty())
		stateRegistry.onEvent(ServiceEvent.Destroy, true)

		if (!MainActivity.isAlive) {
			// could Leak
			// TODO: CleanUp
			notificationManagerService.cancelAll()
			exitProcess(0)
		}
	}

	override fun onUpdateNotification(session: MediaSession) {
		notificationManager.onUpdateNotification(session)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
		return sessionManager.onGetSession(controllerInfo)
	}

	abstract class ServiceComponent {
		private lateinit var impl: ServiceComponent

		private var mCreated = false
		private var mStarted = false
		private var mReleased = false

		private var mServiceDelegate: ServiceDelegate? = null
		private var mComponentDelegate: ComponentDelegate? = null

		protected open val serviceDelegate
			get() = mServiceDelegate

		protected open val componentDelegate
			get() = mComponentDelegate

		open val isInitialized
			get() = ::impl.isInitialized

		open val isCreated
			get() = mCreated

		open val isStarted
			get() = mStarted

		open val isReleased
			get() = mReleased

		@MainThread
		fun createComponent(libraryService: MusicLibraryService) {
			checkMainThread()
			if (isReleased) return

			checkState(!isInitialized)
			initialize()

			checkState(isInitialized)
			create(libraryService.ServiceDelegate())

			checkNotNull(mServiceDelegate)
			mCreated = true
		}

		@MainThread
		fun startComponent(libraryService: MusicLibraryService) {
			checkMainThread()
			if (isReleased || isStarted) return

			checkState(isCreated)
			start(libraryService.ComponentDelegate())

			checkNotNull(mComponentDelegate)
			mStarted = true
		}

		@MainThread
		fun releaseComponent(libraryService: MusicLibraryService) {
			checkMainThread()

			checkState(!isReleased)
			release()

			checkState(mServiceDelegate == null && mComponentDelegate == null)
			mReleased = true
		}

		private var mContextNotified = false

		@MainThread
		fun notifyContextAttached(service: MusicLibraryService) {
			checkMainThread()
			if (mContextNotified || isReleased) return
			serviceContextAttached(service)
		}

		private var mDependencyNotified = false

		@MainThread
		fun notifyDependencyInjected() {
			if (mDependencyNotified || isReleased) return
			serviceDependencyInjected()
			checkState(mDependencyNotified)
		}

		@MainThread
		@CallSuper
		protected open fun initialize() {
			checkMainThread()
			impl = this
		}

		@MainThread
		@CallSuper
		protected open fun create(serviceDelegate: ServiceDelegate) {
			checkMainThread()
			mServiceDelegate = serviceDelegate
		}

		@MainThread
		@CallSuper
		protected open fun start(componentDelegate: ComponentDelegate) {
			checkMainThread()
			mComponentDelegate = componentDelegate
		}

		@MainThread
		@CallSuper
		protected open fun release() {
			checkMainThread()
			mComponentDelegate = null
			mServiceDelegate = null
		}

		@MainThread
		protected open fun serviceContextAttached(context: Context) = Unit

		@MainThread
		@CallSuper
		protected open fun serviceDependencyInjected() {
			mDependencyNotified = true
		}

		abstract class Stoppable : ServiceComponent() {
			private var mStopped = false
			private var mReleasing = false

			val isReleasing
				get() = mReleasing

			override val isStarted: Boolean
				get() = if (!mStopped) super.isStarted else false

			override val componentDelegate: ComponentDelegate?
				get() = if (!mStopped) super.componentDelegate else null

			fun stopComponent(releasing: Boolean) {
				if (isReleased || mStopped) return
				mReleasing = releasing
				stop()
				checkState(!isStarted && componentDelegate == null)
			}

			@MainThread
			@CallSuper
			protected open fun stop() {
				checkMainThread()
				mStopped = true
			}

		}

		abstract class Interactor
	}

	inner class StateInteractor {
		val isForeground
			get() = isServiceForeground

		fun startForeground(notification: Notification) = startForegroundService(notification)
		fun stopForeground(removeNotification: Boolean) = stopForegroundService(removeNotification)

		fun start() {
			if (isServiceCreated && !isServiceStarted && !isServiceReleased) onStart()
		}

		fun stop(release: Boolean) {
			if (isServiceCreated && !isServiceReleased) stopService(release)
		}

		fun release() {
			if (!isServiceReleased) releaseService()
		}
	}

	inner class ServiceDelegate {
		fun getContext(): Context? = baseContext
		fun getInjectedPlayer(): ExoPlayer = injectedExoPlayer
		fun getSessionConnector() = injectedSessionConnector
		fun getStateInteractor() = stateManager.interactor
		fun getSessionInteractor() = sessionManager.interactor
		fun getNotificationManagerInteractor() = notificationManager.interactor
		fun getServiceMainJob(): CompletableJob = serviceJob
	}

	inner class ComponentDelegate {
		@Suppress("UNCHECKED_CAST")
		fun <T : ServiceComponent> get(kls: KClass<T>): T? {
			return serviceComponents.find { it.javaClass.kotlin == kls }?.let { it as T }
		}

		fun <T : ServiceComponent> provide (component: () -> T): Boolean {
			return serviceComponents.add(component())
		}

		fun <T : ServiceComponent> getOrProvide(getKls: KClass<T>, provide: () -> T): T {
			get(getKls) ?: provide(provide)
			return get(getKls)!!
		}
	}

	// Don't Implement LifecycleOwner when there's no use case yet
	private inner class StateRegistry {
		private val mutableServiceStateSF = MutableStateFlow<ServiceState>(ServiceState.Nothing)
		private val mutableServiceEventSF = MutableStateFlow<ServiceEvent>(ServiceEvent.Nothing)

		val serviceStateSF = mutableServiceStateSF.asStateFlow()
		val serviceEventSF = mutableServiceEventSF.asStateFlow()

		fun onEvent(event: ServiceEvent, updateState: Boolean) {

			if (event is ServiceEvent.SingleTimeEvent) {
				checkState(!event.consumed)
				event.consume()
			}

			dispatchEvent(event)
			if (updateState) updateState(event.resultState)
		}

		private fun dispatchEvent(event: ServiceEvent) {
			mutableServiceEventSF.value = event
		}

		private fun updateState(state: ServiceState) {
			val currentState = serviceStateSF.value
			checkState(state upFrom currentState || state downFrom currentState) {
				if (state == currentState) {
					"ServiceState updated multiple times $currentState"
				} else {
					"ServiceState Jump attempt from $currentState to $state"
				}
			}

			when (state) {
				ServiceState.Nothing -> throw IllegalArgumentException()
				ServiceState.Initialized -> checkState(currentState == ServiceState.Nothing)
				else -> Unit
			}

			StateDelegate.updateState(this, state)
			mutableServiceStateSF.value = state
		}
	}

	object StateDelegate : ReadOnlyProperty <Any?, ServiceState> {
		private var savedState: ServiceState = ServiceState.Nothing
		private var stateProvider: Any? = null
		fun updateState(holder: Any, state: ServiceState) {
			checkState(state upFrom savedState || state downFrom savedState) {
				"StateDelegate StateJump Attempt from $savedState to $state"
			}
			when (state) {
				ServiceState.Nothing -> throw IllegalArgumentException()
				ServiceState.Initialized -> {
					checkState(holder !== stateProvider)
					stateProvider = holder
				}
				else -> checkState(stateProvider === holder)
			}
			savedState = state
		}
		override fun getValue(thisRef: Any?, property: KProperty<*>): ServiceState = savedState
	}

	sealed class ServiceState : Comparable<ServiceState> {

		override fun compareTo(other: ServiceState): Int = when {
			ComparableInt.get(this) > ComparableInt.get(other) -> 1
			ComparableInt.get(this) < ComparableInt.get(other) -> -1
			else -> 0
		}

		infix fun upFrom(other: ServiceState): Boolean {
			return ComparableInt.get(this) == (ComparableInt.get(other) - 1)
		}

		infix fun downFrom(other: ServiceState): Boolean {
			return ComparableInt.get(this) == (ComparableInt.get(other) + 1)
		}

		object Nothing : ServiceState()
		object Initialized : ServiceState()
		object ContextAttached : ServiceState()
		object DependencyInjected : ServiceState()
		object Created : ServiceState()
		object Started : ServiceState()
		object Foreground : ServiceState()
		object Paused : ServiceState()
		object Stopped : ServiceState()
		object Released : ServiceState()
		object Destroyed : ServiceState()

		object ComparableInt {
			private const val NOTHING = -1
			private const val INITIALIZED = 0
			private const val CONTEXT_ATTACHED = 1
			private const val DEPENDENCY_INJECTED = 2
			private const val CREATED = 3
			private const val STARTED = 4
			private const val FOREGROUND = 5
			private const val PAUSED = 4
			private const val STOPPED = 3
			private const val Released = 2
			private const val Destroyed = 1

			fun get(serviceState: ServiceState): Int = when (serviceState) {
				ServiceState.Nothing -> NOTHING
				ServiceState.Initialized -> INITIALIZED
				ServiceState.ContextAttached -> CONTEXT_ATTACHED
				ServiceState.DependencyInjected -> DEPENDENCY_INJECTED
				ServiceState.Created -> CREATED
				ServiceState.Started -> STARTED
				ServiceState.Foreground -> FOREGROUND
				ServiceState.Paused -> PAUSED
				ServiceState.Stopped -> STOPPED
				ServiceState.Released -> Released
				ServiceState.Destroyed -> Destroyed
			}
		}
	}

	sealed class ServiceEvent {

		abstract val resultState: ServiceState

		sealed class SingleTimeEvent : ServiceEvent() {
			var consumed: Boolean = false
				private set
			fun consume() {
				consumed = true
			}
		}

		interface LifecycleEvent {
			fun asLifecycleEvent(): Lifecycle.Event
		}

		object Nothing : SingleTimeEvent() {
			override val resultState: ServiceState = ServiceState.Nothing
		}
		object Initialize : SingleTimeEvent() {
			override val resultState: ServiceState = ServiceState.Initialized
		}
		object AttachContext : SingleTimeEvent() {
			override val resultState: ServiceState = ServiceState.ContextAttached
		}
		object InjectDependency : SingleTimeEvent() {
			override val resultState: ServiceState = ServiceState.DependencyInjected
		}
		object Create : SingleTimeEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Created
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_CREATE
		}
		object Start : ServiceEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Started
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_START
		}
		object ResumeForeground : ServiceEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Foreground
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_RESUME
		}
		object PauseForeground : ServiceEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Paused
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_PAUSE
		}
		data class Stop(val isReleasing: Boolean) : ServiceEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Stopped
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_STOP
		}
		object Release : SingleTimeEvent() {
			override val resultState: ServiceState = ServiceState.Released
		}
		object Destroy : SingleTimeEvent(), LifecycleEvent {
			override val resultState: ServiceState = ServiceState.Destroyed
			override fun asLifecycleEvent(): Lifecycle.Event = Lifecycle.Event.ON_DESTROY
		}
	}

	companion object {
		const val MediaNotificationId = 301

		fun getComponentName(): ComponentName = AppDelegate.componentName(MusicLibraryService::class)
	}
}
