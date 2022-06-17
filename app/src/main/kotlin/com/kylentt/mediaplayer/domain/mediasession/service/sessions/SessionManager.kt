package com.kylentt.mediaplayer.domain.mediasession.service.sessions

import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.media3.MediaItemFactory
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService.Companion
import com.kylentt.mediaplayer.domain.mediasession.service.OnChanged
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicLibrarySessionManager(
	private val musicService: MusicLibraryService,
	private val sessionCallback: MediaLibrarySession.MediaLibrarySessionCallback
) {

	private val sessionLock = Any()
	private val mainHandler = Handler(Looper.getMainLooper())

	private lateinit var sessionPlayerHandler: Handler

	/**
	 * Callback when the current [Player] changes, executed on Main Thread as soon as possible
	 *
	 * [MediaSession] change with the same [Player] is not considered a change
	 *
	 * @see [onSessionPlayerChanged]
	 */

	private val onPlayerChangedListener = mutableListOf<OnChanged<Player>>()

	/**
	 * Callback when the current [MediaSession] changes, executed on Main Thread
	 *
	 * [onPlayerChangedListener] is called after if any
	 *
	 * might change when there's multiple MediaSession use case
	 *
	 * @see [Companion.MAX_SESSION]
	 */

	private val onMediaSessionChangedListener = mutableListOf<OnChanged<List<MediaSession>>>()

	/**
	 * [SupervisorJob] for Job's inside this class components.
	 *
	 * Cancelled when this instance is released and or [MusicLibraryService.serviceJob] is cancelled
	 */

	private val sessionManagerJob = SupervisorJob(musicService.serviceJob)

	/**
	 * Default [MediaLibraryService.MediaLibrarySession] Implementation
	 *
	 * might change in the future
	 * @see [MusicLibraryService.Companion.MAX_SESSION]
	 */

	private val localLibrarySessions: MutableList<MediaLibrarySession> = mutableListOf()
	private var sessionPlayer: Player? = null


	var isReleased = false
		private set

	val baseContext
		get() = musicService.baseContext

	val coroutineDispatchers: AppDispatchers = AppModule.provideAppDispatchers()

	val mainImmediateScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.mainImmediate + sessionManagerJob)

	val mainScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.main + sessionManagerJob)

	val isDependencyInjected
		get() = musicService.isDependencyInjected

	val lifecycle
		get() = musicService.lifecycle

	val serviceEventSF
		get() = musicService.serviceEventSF

	private fun addLibrarySession(session: MediaLibrarySession) = synchronized(sessionLock) {
		val shouldAdd = localLibrarySessions.find { it === session } == null

		if (shouldAdd) {
			val old = localLibrarySessions
			localLibrarySessions.add(session)
			onMediaSessionChangedListener.forEach { it.onChanged(old, localLibrarySessions) }

			sessionPlayer?.let { session.player = it }
				?: changeSessionPlayer(session.player)
		}
	}

	@MainThread
	fun initializeSession(service: MusicLibraryService, player: Player) {
		checkMainThread()

		val getLib = buildMediaLibrarySession(musicService, player)
		addLibrarySession(getLib)
	}

	private fun changeSessionPlayerImpl(player: Player) {

		val get: Player?

		synchronized(sessionLock) {
			get = sessionPlayer

			if (get === player) return
			sessionPlayer = player

			if (localLibrarySessions.isEmpty()) return@synchronized
			localLibrarySessions.forEach { it.player = sessionPlayer!! }
		}

		onSessionPlayerChanged(get, player)
	}

	private fun releaseImpl(obj: Any) {
		Timber.d("SessionManager releaseImpl called by $obj")

		sessionManagerJob.cancel()
		this.sessionPlayer = null

		isReleased = true
	}

	private fun onSessionPlayerChanged(old: Player?, new: Player) {
		checkArgument(old !== new)
		checkState(sessionPlayer === new)

		sessionPlayerHandler = Handler(new.applicationLooper)

		immediatePost(sessionPlayerHandler) {
			onPlayerChangedListener.forEach { it.onChanged(old, new) }
		}
	}

	private fun buildMediaLibrarySession(
		musicService: MusicLibraryService,
		player: Player
	): MediaLibrarySession {
		checkNotNull(musicService.baseContext)

		val intent = AppDelegate.launcherIntent()
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val requestCode = MainActivity.Constants.LAUNCH_REQUEST_CODE
		val sessionActivity = PendingIntent.getActivity(musicService.baseContext, requestCode, intent, flag)
		val builder = MediaLibrarySession.Builder(musicService, player, sessionCallback)

		val get = with(builder) {
			setId(MusicLibraryService.Constants.SESSION_ID)
			setSessionActivity(sessionActivity)
			setMediaItemFiller(LocalMediaItemFiller())
			build()
		}

		return get
	}

	private fun immediatePost(handler: Handler, block: () -> Unit) {
		if (Looper.myLooper() === handler.looper) block() else handler.postAtFrontOfQueue(block)
	}

	fun release(obj: Any) {
		if (!this.isReleased) {
			releaseImpl(obj)
		}
	}

	fun getCurrentMediaSession(): MediaSession? {
		if (isReleased) {
			return null
		}

		return localLibrarySessions.lastOrNull()
	}

	fun getSessionPlayer(): Player? = with(sessionPlayer) {
		if (isReleased) {
			checkState(this == null)
		}

		this
	}

	fun getSessionPlayerHandler(): Handler {
		return sessionPlayerHandler
	}

	fun changeSessionPlayer(player: Player) {
		if (isReleased) return
		changeSessionPlayerImpl(player)
	}

	fun registerPlayerChangedListener(onChanged: OnChanged<Player>) {
		if (isReleased) {
			checkState(this.onPlayerChangedListener.isEmpty())
			return
		}

		this.onPlayerChangedListener.add(onChanged)
	}

	fun unregisterPlayerChangedListener(onChanged: OnChanged<Player>): Boolean {
		if (isReleased) {
			return checkState(this.onPlayerChangedListener.isEmpty())
		}

		return this.onPlayerChangedListener.removeAll { it === onChanged }
	}

	fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession? {
		return localLibrarySessions.lastOrNull()
	}

	private inner class LocalMediaItemFiller : MediaSession.MediaItemFiller {
		override fun fillInLocalConfiguration(
			session: MediaSession,
			controller: ControllerInfo,
			mediaItem: MediaItem
		): MediaItem {

			val uri = mediaItem.mediaMetadata.mediaUri

			if (uri == null) {
				Timber.e("MediaItem mediaMetadata.mediaUri should not be null")
				return MediaItemFactory.EMPTY
			}

			return MediaItemFactory.fillInLocalConfig(mediaItem, uri)
		}
	}
}
