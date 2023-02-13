package com.flammky.musicplayer.domain.musiclib.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.Assertions.checkMainThread
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.flammky.android.common.broadcast.ContextBroadcastManager
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.common.kotlin.collection.mutable.forEachClear
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.BuildConfig
import com.flammky.musicplayer.base.activity.ActivityWatcher
import com.flammky.musicplayer.base.media.r.MediaContentWatcher
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasQ
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasSnowCone
import com.flammky.musicplayer.domain.musiclib.service.manager.MediaNotificationManager
import com.flammky.musicplayer.domain.musiclib.service.manager.PlaybackManager
import com.flammky.musicplayer.domain.musiclib.service.manager.SessionManager
import com.flammky.musicplayer.domain.musiclib.service.manager.StateManager
import com.flammky.musicplayer.domain.musiclib.service.provider.SessionProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

@AndroidEntryPoint
@Deprecated("was sandbox, todo rework")
class MusicLibraryService : MediaLibraryService() {

	@Inject
	lateinit var iMediaMetadataCacheRepository: MediaMetadataCacheRepository

	@Inject
	lateinit var iMediaStoreProvider: MediaStoreProvider

	@Inject
	lateinit var mediaContentWatcher: MediaContentWatcher

	@Inject
	@Named("MusicExoPlayer")
	lateinit var iExoPlayer: ExoPlayer

	private lateinit var notificationManagerService: NotificationManager
	private lateinit var broadcastManager: ContextBroadcastManager

	private var mReleasing = false

	private val appDispatchers = AndroidCoroutineDispatchers.DEFAULT
	private val serviceJob = SupervisorJob()
	private val serviceScope = CoroutineScope(appDispatchers.main + serviceJob)

	private val stateRegistry = StateRegistry()
	private val componentManager = ComponentManager()

	private val sessionManager = SessionManager(SessionProvider(this))
	private val stateManager = StateManager(StateInteractor())
	private val notificationManager = MediaNotificationManager(MediaNotificationId)
	private val playbackManager = PlaybackManager()

	private val currentState
		get() = stateRegistry.serviceStateSF.value

	private val isServiceCreated get() = currentState >= ServiceState.Created
	private val isServiceStarted get() = currentState >= ServiceState.Started
	private val isServiceForeground get() = currentState == ServiceState.Foreground
	private val isServicePaused get() = currentState <= ServiceState.Paused
	private val isServiceStopped get() = currentState <= ServiceState.Stopped
	private val isServiceReleased get() = currentState <= ServiceState.Released
	private val isServiceDestroyed get() = currentState == ServiceState.Destroyed

	init {
		componentManager.add(sessionManager)
		componentManager.add(stateManager)
		componentManager.add(playbackManager)
		componentManager.add(notificationManager)
		onPostInitialize()
	}

	private fun onPostInitialize() {
		stateRegistry.onEvent(ServiceEvent.Initialize, true)
		componentManager.start()
	}

	override fun onCreate() {
		onContextAttached()
		super.onCreate()
		onDependencyInjected()
		postCreate()
	}

	private fun onContextAttached() {
		broadcastManager = ContextBroadcastManager(this)
		notificationManagerService = getSystemService(NotificationManager::class.java)
		stateRegistry.onEvent(ServiceEvent.AttachContext, true)
	}

	private fun onDependencyInjected() {
		stateRegistry.onEvent(ServiceEvent.InjectDependency, true)
	}

	private fun postCreate() {
		stateRegistry.onEvent(ServiceEvent.Create, true)
	}

	private fun onStart() {
		stateRegistry.onEvent(ServiceEvent.Start, true)
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
		try {
			if (!isServiceStarted) onStart()
			if (AndroidAPI.hasQ()) {
				startForeground(
					MediaNotificationId, notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
				)
			} else {
				startForeground(MediaNotificationId, notification)
			}
			if (!isServiceForeground) {
				stateRegistry.onEvent(ServiceEvent.ResumeForeground, true)
			}
		} catch (e: Exception) {
			Timber.e(e)
			if (BuildConfig.DEBUG
				|| (AndroidAPI.hasSnowCone() && e !is ForegroundServiceStartNotAllowedException)
			) throw e
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
		stopSelf()

		stateRegistry.onEvent(ServiceEvent.Stop(release), true)
		if (release) releaseService()
	}

	private fun releaseService() {
		if (!isServiceStopped) stopService(true)
		stateRegistry.onEvent(ServiceEvent.Release, true)
		releaseComponent()
		releaseSessions()
		serviceJob.cancel()
	}

	private fun releaseComponent() {
		broadcastManager.release()
		componentManager.release()
		iExoPlayer.release()
	}

	private fun releaseSessions() {
		sessions.forEachClear { it.release() }
	}

	override fun onDestroy() {
		if (!isServiceReleased) releaseService()
		super.onDestroy()
		postDestroy()
	}

	private fun postDestroy() {
		stateRegistry.onEvent(ServiceEvent.Destroy, true)
		check(componentManager.released)

		// probably would be nice to introduce something similar in musiclib package

		// we should have something else instead
		if (ActivityWatcher.get().count() == 0) {
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
		private var mCreated = false
		private var mStarted = false
		private var mReleased = false
		private var mContextNotified = false
		private var mDependencyNotified = false

		private lateinit var mServiceDelegate: ServiceDelegate
		private lateinit var mComponentDelegate: ComponentDelegate

		protected open val serviceDelegate
			get() = mServiceDelegate

		protected open val componentDelegate
			get() = mComponentDelegate

		open val isCreated
			get() = mCreated

		open val isStarted
			get() = mStarted

		open val isReleased
			get() = mReleased

		@MainThread
		fun createComponent(libraryService: MusicLibraryService) {
			checkMainThread()
			if (mCreated || mReleased) return
			create(libraryService.ServiceDelegate())
			check(::mServiceDelegate.isInitialized) {
				"ServiceComponent did not call super.create(): $this"
			}
			mCreated = true
		}

		@MainThread
		fun startComponent(libraryService: MusicLibraryService) {
			checkMainThread()
			if (mStarted || mReleased) return
			if (!mCreated) createComponent(libraryService)
			if (!mContextNotified) notifyContextAttached(libraryService)
			if (!mDependencyNotified) notifyDependencyInjected(libraryService)

			start(libraryService.ComponentDelegate())
			check(::mComponentDelegate.isInitialized) {
				"ServiceComponent did not call super.start(): $this"
			}
			mStarted = true
		}

		@MainThread
		fun releaseComponent() {
			checkMainThread()
			if (mReleased) return
			release()
			mReleased = true
		}

		@MainThread
		fun notifyContextAttached(service: MusicLibraryService) {
			checkMainThread()
			if (mContextNotified || isReleased) return
			if (!isCreated) createComponent(service)
			serviceContextAttached(service)
		}

		@MainThread
		fun notifyDependencyInjected(service: MusicLibraryService) {
			checkMainThread()
			if (mDependencyNotified || isReleased) return
			if (!isCreated) createComponent(service)
			serviceDependencyInjected()
		}

		@MainThread
		@CallSuper
		protected open fun create(serviceDelegate: ServiceDelegate) {
			mServiceDelegate = serviceDelegate
		}

		@MainThread
		@CallSuper
		protected open fun start(componentDelegate: ComponentDelegate) {
			mComponentDelegate = componentDelegate
		}

		@MainThread
		protected open fun release() {
		}

		@MainThread
		@CallSuper
		protected open fun serviceContextAttached(context: Context) {
			mContextNotified = true
		}

		@MainThread
		@CallSuper
		protected open fun serviceDependencyInjected() {
			mDependencyNotified = true
		}

		abstract class Interactor
	}

	inner class StateInteractor {
		val isForeground
			get() = isServiceForeground

		fun startForeground(notification: Notification) {
			if (isServiceCreated) startForegroundService(notification)
		}

		fun stopForeground(removeNotification: Boolean) {
			if (isServiceCreated) stopForegroundService(removeNotification)
		}

		fun start() {
			if (isServiceCreated && !isServiceStarted) onStart()
		}

		fun stop(release: Boolean) {
			if (isServiceCreated) stopService(release)
		}

		fun release() {
			if (!isServiceReleased) releaseService()
		}
	}

	inner class ServiceDelegate {
		val property = Property()
	}

	inner class Property {
		private val service = this@MusicLibraryService

		val context: Context? get() = if (baseContext != null) service else null
		val injectedPlayer: ExoPlayer get() = iExoPlayer
		val serviceDispatchers get() = appDispatchers
		val serviceMainJob get() = serviceJob
		val mediaConnectionRepository get() = iMediaMetadataCacheRepository
		val mediaStoreProvider: MediaStoreProvider get() = iMediaStoreProvider
	}

	inner class ComponentDelegate {
		val sessionInteractor get() = sessionManager.interactor
		val stateInteractor get() = stateManager.interactor
		val notificationInteractor get() = notificationManager.interactor
	}

	// Don't Implement LifecycleOwner when there's no use case yet
	private inner class StateRegistry {
		private val mutableServiceStateSF = MutableStateFlow<ServiceState>(ServiceState.Nothing)
		private val mutableServiceEventSF = MutableStateFlow<ServiceEvent>(ServiceEvent.Nothing)

		val serviceStateSF = mutableServiceStateSF.asStateFlow()
		val serviceEventSF = mutableServiceEventSF.asStateFlow()

		fun onEvent(event: ServiceEvent, updateState: Boolean) {

			Timber.d("StateRegistry onEvent\nevent: $event")

			if (event is ServiceEvent.SingleTimeEvent) {
				check(!event.consumed)
				event.consume()
			}

			dispatchEvent(event)
			if (updateState) updateState(event.resultState)
		}

		private fun dispatchEvent(event: ServiceEvent) {
			mutableServiceEventSF.value = event
		}

		fun updateState(state: ServiceState) {
			val currentState = serviceStateSF.value
			check(state upFrom currentState || state downFrom currentState) {
				if (state == currentState) {
					"ServiceState updated multiple times $currentState"
				} else {
					"ServiceState Jump attempt from $currentState to $state"
				}
			}

			when (state) {
				ServiceState.Nothing -> throw IllegalArgumentException()
				ServiceState.Initialized -> check(currentState == ServiceState.Nothing)
				else -> Unit
			}

			StateDelegate.updateState(this, state)
			mutableServiceStateSF.value = state
		}
	}

	@MainThread
	private inner class ComponentManager {
		private val components = mutableSetOf<ServiceComponent>()

		// Explicit reference
		private val service = this@MusicLibraryService

		var started = false
			private set

		var released = false
			private set

		fun add(component: ServiceComponent) {
			checkMainThread()
			if (released) return
			components.add(component)
			notifyComponent(component, service.currentState)
		}

		fun remove(component: ServiceComponent) {
			checkMainThread()
			if (released) return
			component.releaseComponent()
			components.remove(component)
		}

		fun start() {
			checkMainThread()
			if (started || released) return
			serviceScope.launch {
				stateRegistry.serviceEventSF
					.safeCollect { event ->
						components.forEach { notifyComponent(it, event.resultState) }
					}
			}
			started = true
		}

		fun release() {
			checkMainThread()
			if (released) return
			components.forEachClear { it.releaseComponent() }
			released = true
		}

		private fun notifyComponent(component: ServiceComponent, state: ServiceState) {
			when (state) {
				ServiceState.Nothing -> Unit
				ServiceState.Initialized -> component.createComponent(service)
				ServiceState.ContextAttached -> component.notifyContextAttached(service)
				ServiceState.DependencyInjected -> component.notifyDependencyInjected(service)
				ServiceState.Created, ServiceState.Stopped, ServiceState.Started, ServiceState.Paused,
				ServiceState.Foreground -> component.startComponent(service)
				ServiceState.Released, ServiceState.Destroyed -> component.releaseComponent()
			}
		}
	}

	object StateDelegate : ReadOnlyProperty<Any?, ServiceState> {
		private var savedState: ServiceState = ServiceState.Nothing
		private var stateProvider: Any? = null
		fun updateState(holder: Any, state: ServiceState) {
			when (state) {
				ServiceState.Nothing -> throw IllegalArgumentException()
				ServiceState.Initialized -> {
					check(holder !== stateProvider)
					stateProvider = holder
				}
				ServiceState.Destroyed -> {
					check(holder === stateProvider)
					stateProvider = null
				}
				else -> {
					check(holder === stateProvider)
					check(state upFrom savedState || state downFrom savedState) {
						"StateDelegate StateJump Attempt from $savedState to $state"
					}
				}
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
				Nothing -> NOTHING
				Initialized -> INITIALIZED
				ContextAttached -> CONTEXT_ATTACHED
				DependencyInjected -> DEPENDENCY_INJECTED
				Created -> CREATED
				Started -> STARTED
				Foreground -> FOREGROUND
				Paused -> PAUSED
				Stopped -> STOPPED
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

		fun getComponentName(context: Context): ComponentName {
			return ComponentName(context.packageName, MusicLibraryService::class.java.name)
		}
	}
}
