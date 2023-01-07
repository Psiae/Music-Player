package com.flammky.musicplayer.base.media.mediaconnection.playback.real

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackSessionConnector
import com.flammky.musicplayer.base.user.User
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

class RealPlaybackConnection(
	private val authService: AuthService,
	private val looper: Looper,
	private val context: Context,
) : PlaybackConnection {

	private val supervisorDispatcher: MainCoroutineDispatcher = Handler(looper).asCoroutineDispatcher()
	private val supervisorContext = supervisorDispatcher + SupervisorJob()
	private val supervisorScope = CoroutineScope(supervisorContext)

	private val _sessionLock = ReentrantLock(true)
	private val _sessionMap = mutableMapOf<String, PlaybackSessionConnector>()

	override fun requestUserSessionAsync(
		user: User,
		coroutineContext: CoroutineContext
	): Deferred<PlaybackSessionConnector> {
		return supervisorScope.async(coroutineContext + supervisorDispatcher) {

			val state = authService.state

			if (state is AuthService.AuthState.LoggedIn && state.user == user) {
				_sessionLock.withLock {
					_sessionMap.getOrPut(user.uid) {
						PlaybackSessionConnector(user.uid, context, looper)
					}
				}.apply connector@ {
					connect().onSuccess { return@async this@connector }
				}
			}

			// should throw rejection
			throw CancellationException()
		}
	}
}
