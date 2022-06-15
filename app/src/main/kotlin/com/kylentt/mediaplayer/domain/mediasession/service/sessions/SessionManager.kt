package com.kylentt.mediaplayer.domain.mediasession.service.sessions

import android.app.PendingIntent
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSession.MediaItemFiller
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService.Companion
import com.kylentt.mediaplayer.domain.mediasession.service.OnChanged
import com.kylentt.mediaplayer.domain.mediasession.service.notification.MusicLibraryNotificationProvider
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MusicLibrarySessionManager(
	private val musicService: MusicLibraryService,
	private val mediaItemFiller: MediaItemFiller,
	private val sessionCallback: MediaLibrarySession.MediaLibrarySessionCallback
) {

	private val sessionLock = Any()
	private val mainHandler = Handler(Looper.getMainLooper())

	/**
	 * Callback when the current [Player] changes, executed on Main Thread
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

	private val mediaLibrarySession: MediaLibrarySession by lazy {
		checkNotNull(baseContext)

		val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
		val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		val requestCode = MainActivity.Constants.LAUNCH_REQUEST_CODE
		val sessionActivity = PendingIntent.getActivity(baseContext, requestCode, intent, flag)
		val builder = MediaLibrarySession
			.Builder(musicService, musicService.injectedPlayer, sessionCallback)

		val get = with(builder) {
			setId(MusicLibraryService.Constants.SESSION_ID)
			setSessionActivity(sessionActivity)
			setMediaItemFiller(mediaItemFiller)
			build()
		}

		onSessionPlayerChanged(sessionPlayer, get.player)
		get
	}

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

	val mediaSession
		get() = mediaLibrarySession

	val isDependencyInjected
		get() = musicService.isDependencyInjected

	val lifecycle
		get() = musicService.lifecycle

	val serviceEventSF
		get() = musicService.serviceEventSF

	init {}

	private fun releaseImpl(obj: Any) {
		sessionManagerJob.cancel()
		this.sessionPlayer = null

		isReleased = true
	}

	@MainThread
	private fun onSessionPlayerChanged(old: Player?, new: Player) {
		checkArgument(old !== new)

		mainHandler.post {
			checkMainThread()
			onPlayerChangedListener.forEach { it.onChanged(old, new) }
		}
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
		return mediaLibrarySession
	}

	fun getSessionPlayer(): Player? = with(sessionPlayer) {
		if (isReleased) {
			checkState(this == null)
			null
		} else {
			this
		}
	}

	fun changeSessionPlayer(player: Player) {
		val get = this.sessionPlayer

		if (isReleased) {
			if (get != null) {
				Timber.e("sessionPlayer was not null on released State")
				this.sessionPlayer = null
			}
			return
		}

		synchronized(sessionLock) {
			if (get !== player) this.sessionPlayer = player else return
			if (musicService.sessions.isNotEmpty()) {
				musicService.sessions.forEach { it.player = player }
			}
		}

		onSessionPlayerChanged(get, player)
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

	fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession = this.mediaLibrarySession
}
