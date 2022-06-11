package com.kylentt.mediaplayer.domain.mediasession.service
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.Lifecycle.State.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.app.delegates.AppDelegate
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
	fun onChanged(old: T?, new: T)
}

interface LifecycleEvent {
	fun asLifecycleEvent(): Lifecycle.Event
}

@AndroidEntryPoint
class MusicLibraryService : MediaLibraryService() {

	@Inject lateinit var coilHelper: CoilHelper
	@Inject lateinit var coroutineDispatchers: AppDispatchers
	@Inject lateinit var injectedPlayer: ExoPlayer
	@Inject lateinit var mediaItemHelper: MediaItemHelper
	@Inject lateinit var mediaRepository: MediaRepository
	@Inject lateinit var sessionConnector: MediaSessionConnector

	/**
	 * Callback when the current [Player] changes
	 *
	 * [MediaSession] change with the same [Player] is not considered a change
	 *
	 * use [onMediaSessionChangedListener] instead
	 */

	private val onPlayerChangedListener: MutableList<OnChanged<Player>> = mutableListOf()

	/**
	 * Callback when the current [MediaSession] changes
	 *
	 * [onPlayerChangedListener] is called after if any
	 *
	 * might change when there's multiple MediaSession use case, for now restrict to only one session

	 * @see [Companion.MAX_SESSION]
	 */

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
		MusicLibraryServiceListener(this, mediaEventHandler, stateRegistry)
	}

	val serviceStateSF = stateRegistry.serviceStateSF
	val serviceEventSF = stateRegistry.serviceEventSF
	val mediaStateSF = stateRegistry.mediaStateSF

	val mainImmediateScope by lazy {
		CoroutineScope(coroutineDispatchers.mainImmediate + SupervisorJob())
	}
	val mainScope by lazy {
		CoroutineScope(coroutineDispatchers.main + SupervisorJob())
	}
	val ioScope by lazy {
		CoroutineScope(coroutineDispatchers.io + SupervisorJob())
	}

	val currentMediaSession: MediaSession
		get() = mediaLibrarySession

	val isServiceForeground
		get() = serviceStateSF.value.isForeground()

	init {
		stateRegistry.onEvent(ServiceEvent.ON_INITIALIZE)
	}

	override fun onCreate() {
		Timber.i("Service onCreate()")

		setupEventListener()

		stateRegistry.onEvent(ServiceEvent.ON_CREATE)
		super.onCreate()

		setupNotificationProvider()

		// Player Injected
		onPlayerChangedListener.forEach { it.onChanged(null, injectedPlayer) }
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Timber.i("Service onStartCommand(), ${intent}\n${flags}\n${startId}")

		stateRegistry.onEvent(ServiceEvent.ON_START_COMMAND)

		val get =
			try {
				super.onStartCommand(intent, flags, startId)
			} catch (e: IllegalStateException) {
				START_STICKY
			}

		return get
	}

	override fun onBind(intent: Intent?): IBinder? {
		Timber.i("Service onBind()")
		stateRegistry.onEvent(ServiceEvent.ON_BIND)
		return super.onBind(intent)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
		Timber.i("Service onGetSession()")
		stateRegistry.updateState(MediaState.INITIALIZED)
		return mediaLibrarySession
	}

	override fun onDestroy() {
		Timber.i("MusicLibraryService onDestroy() is called")
		stateRegistry.onEvent(ServiceEvent.ON_DESTROY)
		cancelServiceScope()
		releaseComponent()
		releaseSession()
		super.onDestroy()
	}

	private fun setupNotificationProvider() {
		setMediaNotificationProvider(mediaNotificationProvider)
	}

	private fun setupEventListener() {
		mediaEventListener.start(stopSelf = true, releaseSelf = true)
	}

	private fun cancelServiceScope() {
		ioScope.cancel()
		mainScope.cancel()
		mainImmediateScope.cancel()
	}

	private fun releaseComponent() {
		if (injectedPlayer !== mediaLibrarySession.player) {
			injectedPlayer.release()
		}
		mediaLibrarySession.player.release()
		mediaEventListener.release()
	}

	private fun releaseSession() {
		checkState(!serviceStateSF.value.isForeground()) {
			"tried to releaseSession in Foreground State"
		}
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

		if (isEvent) stateRegistry.onEvent(ServiceEvent.ON_FOREGROUND)
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
		if (isEvent) stateRegistry.onEvent(ServiceEvent.ON_PAUSE)
	}

	private fun stopServiceImpl(releaseSession: Boolean) {
		stateRegistry.onEvent(ServiceEvent.ON_STOP)
		if (releaseSession) this.sessions.forEach { it.release() }
	}

	fun startForegroundService(id: Int, notification: Notification, isEvent: Boolean = false) {
		Timber.d("startForegroundService, isEvent: $isEvent")

		if (isEvent) {
			// shouldn't be called by Notification Validator
			checkState(!serviceStateSF.value.isForeground())
		}
		startForegroundServiceImpl(id, notification, isEvent)
	}

	fun stopForegroundService(id: Int, removeNotification: Boolean, isEvent: Boolean = false) {
		Timber.d("stopForegroundService, isEvent: $isEvent")

		if (isEvent) {
			// shouldn't be called by Notification Validator
			checkState(serviceStateSF.value.isForeground())
		}
		stopForegroundServiceImpl(id, removeNotification, isEvent)
	}

	fun stopService(releaseSession: Boolean) {
		if (serviceStateSF.value.isForeground()) {
			stopForegroundService(mediaNotificationProvider.mediaNotificationId,
				removeNotification = true, isEvent = true
			)
		}
		stopServiceImpl(releaseSession)
	}

	fun registerOnPlayerChanged(onChanged: OnChanged<Player>) {
		this.onPlayerChangedListener.add(onChanged)
	}

	fun unRegisterOnPlayerChanged(onChanged: OnChanged<Player>): Boolean {
		return this.onPlayerChangedListener.removeAll { it === onChanged }
	}

	fun registerOnMediaSessionChanged(onChanged: OnChanged<MediaSession>) {
		this.onMediaSessionChangedListener.add(onChanged)
	}

	fun unRegisterOnMediaSessionChanged(onChanged: OnChanged<MediaSession>): Boolean {
		return this.onMediaSessionChangedListener.removeAll { it === onChanged }
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

	/**
	 * Manage [MusicLibraryService] State,
	 *
	 * Observable [LifecycleOwner] and Collectable [StateFl]
	 */

	private inner class StateRegistry : LifecycleOwner {

		private val lifecycleRegistry = LifecycleRegistry(this)

		private val _serviceEventSF: MutableStateFlow<ServiceEvent> = MutableStateFlow(ServiceEvent.ON_STOP)
		private val _serviceStateSF: MutableStateFlow<STATE> = MutableStateFlow(STATE.NOTHING)
		private val _mediaStateSF: MutableStateFlow<MediaState> = MutableStateFlow(MediaState.NOTHING)

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
			_serviceEventSF.value = event

			if (event is LifecycleEvent) {
				onEvent(event.asLifecycleEvent())
			}

			updateState(event)
		}

		private fun updateState(event: ServiceEvent) {
			when (event) {
				ServiceEvent.ON_INITIALIZE -> updateState(STATE.INITIALIZED)

				ServiceEvent.ON_CREATE -> updateState(STATE.CREATED)

				ServiceEvent.ON_DESTROY -> updateState(STATE.DESTROYED)

				ServiceEvent.ON_START_COMMAND -> {
					if (serviceState sameAs STATE.CREATED) updateState(STATE.STARTED)
				}

				ServiceEvent.ON_BIND -> updateState(STATE.STARTED)

				ServiceEvent.ON_PAUSE -> updateState(STATE.PAUSED)

				ServiceEvent.ON_STOP -> updateState(STATE.PAUSED)

				ServiceEvent.ON_FOREGROUND -> updateState(STATE.FOREGROUND)
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

					if (state moreThan serviceState) {
						checkState( state upFrom serviceState ) {
							"State jump from $serviceState to $state"
						}
					} else {
						checkState( state downFrom serviceState ) {
							"State jump from $serviceState to $state"
						}
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
					Event.ON_CREATE -> ON_CREATE
					ON_START -> ON_START_COMMAND
					ON_RESUME -> ON_FOREGROUND
					Event.ON_PAUSE -> ON_PAUSE
					Event.ON_STOP -> ON_STOP
					Event.ON_DESTROY -> ON_DESTROY
					ON_ANY -> TODO()
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

		infix fun atLeast(that: STATE): Boolean = Int.get(this) >= Int.get(that)
		infix fun atMost(that: STATE): Boolean = Int.get(this) <= Int.get(that)
		infix fun lessThan(that: STATE): Boolean = Int.get(this) < Int.get(that)
		infix fun moreThan(that: STATE): Boolean = Int.get(this) > Int.get(that)
		infix fun sameAs(that: STATE): Boolean = Int.get(this) == Int.get(that)

		infix fun downFrom(that: STATE): Boolean = Int.get(this) == (Int.get(that) - 1)
		infix fun upFrom(that: STATE): Boolean = Int.get(this) == (Int.get(that) + 1)

		fun isForeground(): Boolean = this == FOREGROUND

		private object Int {
			const val Nothing = -1
			const val Destroyed = 0
			const val Initialized = 1
			const val Created = 2
			const val Started = 3
			const val Foreground = 4
			const val Paused = 3
			const val Stopped = 2

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
