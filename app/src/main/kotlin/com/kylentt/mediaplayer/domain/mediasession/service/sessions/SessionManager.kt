package com.kylentt.mediaplayer.domain.mediasession.service.sessions

import android.app.PendingIntent
import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.dependency.AppModule
import com.kylentt.mediaplayer.core.OnChangedNotNull
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

class MusicLibrarySessionManager(
	private val musicLibrary: MusicLibraryService,
	private val sessionCallback: MediaLibrarySession.Callback
) {

	private val sessionRegistry = SessionRegistry()
	private val sessionManagerJob = SupervisorJob(musicLibrary.serviceJob)

	private var componentInteractor: MusicLibraryService.ComponentInteractor? = null

	var isReleased: Boolean = false
		private set(value) {
			checkArgument(value) {
				"cannot unRelease this class"
			}
			field = value
		}

	var isStarted: Boolean = false
		private set

	val coroutineDispatchers: AppDispatchers = AppModule.provideAppDispatchers()

	val mainImmediateScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.mainImmediate + sessionManagerJob)

	val mainScope: CoroutineScope =
		CoroutineScope(coroutineDispatchers.main + sessionManagerJob)

	@MainThread
	fun start(
		componentInteractor: MusicLibraryService.ComponentInteractor,
		player: Player
	) {
		checkMainThread()

		if (isReleased || isStarted) return

		if (sessionRegistry.isLibrarySessionInitialized) {
			return Timber.w("Tried to Initialize LibrarySession Multiple Times")
		}

		if (this.componentInteractor != componentInteractor) {
			this.componentInteractor = componentInteractor
		}

		val get = sessionRegistry.buildMediaLibrarySession(musicLibrary, player)
		sessionRegistry.changeLocalLibrarySession(get)

		isStarted = true
	}

	@MainThread
	fun stop() {
		checkMainThread()

		if (isReleased || !isStarted) return

		this.componentInteractor = null

		// TODO: do something about the session and or player

		isStarted = false
	}

	@MainThread
	fun release(obj: Any) {
		Timber.i("SessionManager.release() called by $obj")

		checkMainThread()

		if (isReleased) return
		if (isStarted) stop()

		sessionManagerJob.cancel()
		sessionRegistry.release()

		isReleased = true

		Timber.i("SessionManager released by $obj")
	}

	fun getCurrentMediaSession(): MediaSession? {
		if (isReleased || !sessionRegistry.isLibrarySessionInitialized) return null

		return sessionRegistry.localLibrarySession
	}

	fun getSessionPlayer(): Player? {
		if (isReleased || !sessionRegistry.isLibrarySessionInitialized) return null

		return sessionRegistry.localLibrarySession.player
	}

	fun changeSessionPlayer(player: Player, release: Boolean) {
		if (isReleased) return

		sessionRegistry.changeSessionPlayer(player, release)
	}

	fun registerPlayerChangedListener(onChanged: OnChangedNotNull<Player>) {
		if (isReleased) return

		sessionRegistry.registerOnPlayerChangedListener(onChanged)
	}

	fun unregisterPlayerChangedListener(onChanged: OnChangedNotNull<Player>): Boolean {
		if (isReleased) return false

		return sessionRegistry.unRegisterOnPlayerChangedListener(onChanged)
	}

	fun registerPlayerEventListener(listener: Player.Listener) {
		if (isReleased) return

		sessionRegistry.registerOnPlayerEventListener(listener)
	}

	fun unRegisterPlayerEventListener(listener: Player.Listener): Boolean {
		if (isReleased) return false

		return sessionRegistry.unregisterOnPlayerEventListener(listener)
	}

	fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession? {
		return if (!isReleased) sessionRegistry.localLibrarySession else null
	}

	private inner class SessionRegistry {

		lateinit var localLibrarySession: MediaLibrarySession
			private set

		private val onLibrarySessionChangedListener: MutableList<OnChangedNotNull<MediaLibrarySession>> =
			mutableListOf()

		private val onPlayerChangedListener: MutableList<OnChangedNotNull<Player>> =
			mutableListOf()

		private val onPlayerEventListener: MutableList<Player.Listener> =
			mutableListOf()

		/**
		 * MediaLibrarySession status will be tracked manually as the library didn't provide one
		 */

		var isLibrarySessionReleased = false
			private set

		val isLibrarySessionInitialized
			get() = ::localLibrarySession.isInitialized

		private fun onSessionPlayerChanged(old: Player?, new: Player) {
			checkArgument(old !== new)
			checkState(localLibrarySession.player === new)

			onPlayerEventListener.forEach { listener ->
				old?.removeListener(listener)
				new.addListener(listener)
			}

			onPlayerChangedListener.forEach { it.onChanged(old, new) }
		}

		fun changeLocalLibrarySession(session: MediaLibrarySession) {
			var oldSession: MediaLibrarySession? = null
			var oldPlayer: Player? = null

			if (isLibrarySessionInitialized) {
				if (localLibrarySession === session) {
					return Timber.w("Tried to change LocalLibrarySession to same Instance." +
						"\n $localLibrarySession === $session")
				}

				oldSession = localLibrarySession
				oldPlayer = localLibrarySession.player
			}

			localLibrarySession = session

			oldSession?.release()

			onLibrarySessionChangedListener.forEach { it.onChanged(oldSession, session) }

			if (session.player !== oldPlayer) {
				oldPlayer?.release()
				onSessionPlayerChanged(oldPlayer, session.player)
			}
		}

		fun changeSessionPlayer(player: Player, release: Boolean) {
			val old = localLibrarySession.player

			if (player === old) return
			if (release) old.release()

			localLibrarySession.player = player
			onSessionPlayerChanged(old, player)
		}

		fun registerOnLibrarySessionChangedListener(listener: OnChangedNotNull<MediaLibrarySession>) {
			onLibrarySessionChangedListener.add(listener)
		}

		fun registerOnPlayerChangedListener(listener: OnChangedNotNull<Player>) {
			onPlayerChangedListener.add(listener)
		}

		fun registerOnPlayerEventListener(listener: Player.Listener) {
			if (::localLibrarySession.isInitialized) {
				localLibrarySession.player.addListener(listener)
			}

			onPlayerEventListener.add(listener)
		}

		fun unRegisterOnLibrarySessionChangedListener(listener: OnChangedNotNull<MediaLibrarySession>): Boolean {
			return onLibrarySessionChangedListener.removeAll { it === listener }
		}

		fun unRegisterOnPlayerChangedListener(listener: OnChangedNotNull<Player>): Boolean {
			return onPlayerChangedListener.removeAll { it === listener }
		}

		fun unregisterOnPlayerEventListener(listener: Player.Listener): Boolean {
			if (::localLibrarySession.isInitialized) {
				localLibrarySession.player.removeListener(listener)
			}

			return onPlayerEventListener.removeAll { it === listener }
		}

		fun buildMediaLibrarySession(
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
				build()
			}

			return get
		}

		fun releaseSession() {
			localLibrarySession.release()
			isLibrarySessionReleased = true
		}

		fun release() {
			onLibrarySessionChangedListener.clear()
			onPlayerChangedListener.clear()
			releaseSession()
		}
	}

	inner class Delegate {

		val sessionPlayer: Player?
			get() = getSessionPlayer()

		val mediaSession: MediaSession?
			get() = getCurrentMediaSession()

		fun registerPlayerChangedListener(listener: OnChangedNotNull<Player>): Unit =
			this@MusicLibrarySessionManager.registerPlayerChangedListener(listener)

		fun removePlayerChangedListener(listener: OnChangedNotNull<Player>): Boolean =
			this@MusicLibrarySessionManager.unregisterPlayerChangedListener(listener)

		fun registerPlayerEventListener(listener: Player.Listener) =
			this@MusicLibrarySessionManager.registerPlayerEventListener(listener)

		fun removePlayerEventListener(listener: Player.Listener) =
			this@MusicLibrarySessionManager.unRegisterPlayerEventListener(listener)
	}
}
