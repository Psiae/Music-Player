package com.kylentt.mediaplayer.domain.mediasession.service.sessions

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.core.OnChangedNotNull
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import kotlinx.coroutines.Job
import timber.log.Timber

class SessionManager(
	private val service: MusicLibraryService,
	private val sessionCallback: MediaLibrarySession.Callback
) : MusicLibraryService.ServiceComponent.Stoppable() {

	private val sessionRegistry = SessionRegistry()

	private lateinit var sessionManagerJob: Job

	override fun onCreate(serviceInteractor: MusicLibraryService.ServiceInteractor) {
		super.onCreate(serviceInteractor)
		sessionManagerJob = serviceInteractor.getCoroutineMainJob()
	}

	override fun onDependencyInjected() {
		super.onDependencyInjected()
		if (!sessionRegistry.isLibrarySessionInitialized) {
			val get = sessionRegistry
				.buildMediaLibrarySession(
					serviceInteractor!!.getContext()!!,
					service,
					service.injectedPlayer
				)
			sessionRegistry.changeLocalLibrarySession(get)
		}
	}

	override fun onStart(
		componentInteractor: MusicLibraryService.ComponentInteractor
	) {
		super.onStart(componentInteractor)
	}

	@MainThread
	override fun onStop(componentInteractor: MusicLibraryService.ComponentInteractor) {
		super.onStop(componentInteractor)
		// TODO: do something about the session and or player
	}

	@MainThread
	override fun onRelease(obj: Any) {
		Timber.i("SessionManager.release() called by $obj")
		super.onRelease(obj)

		sessionManagerJob.cancel()
		sessionRegistry.release()

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
					return Timber.w(
						"Tried to change LocalLibrarySession to same Instance." +
							"\n $localLibrarySession === $session"
					)
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
			context: Context,
			libraryService: MediaLibraryService,
			player: Player
		): MediaLibrarySession {
			val intent = AppDelegate.launcherIntent()
			val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			val requestCode = MainActivity.Constants.LAUNCH_REQUEST_CODE
			val sessionActivity = PendingIntent.getActivity(context, requestCode, intent, flag)
			val builder = MediaLibrarySession.Builder(libraryService, player, sessionCallback)

			return with(builder) {
				setId(MusicLibraryService.Constants.SESSION_ID)
				setSessionActivity(sessionActivity)
				build()
			}
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
			this@SessionManager.registerPlayerChangedListener(listener)

		fun removePlayerChangedListener(listener: OnChangedNotNull<Player>): Boolean =
			this@SessionManager.unregisterPlayerChangedListener(listener)

		fun registerPlayerEventListener(listener: Player.Listener) =
			this@SessionManager.registerPlayerEventListener(listener)

		fun removePlayerEventListener(listener: Player.Listener) =
			this@SessionManager.unRegisterPlayerEventListener(listener)
	}
}
