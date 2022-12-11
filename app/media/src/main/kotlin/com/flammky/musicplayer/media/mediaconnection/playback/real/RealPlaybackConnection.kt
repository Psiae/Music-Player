package com.flammky.musicplayer.media.mediaconnection.playback.real

import android.os.Handler
import android.os.Looper
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.playback.PlaybackSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RealPlaybackConnection(
	private val looper: Looper
) : PlaybackConnection {

	private val supervisorDispatcher: MainCoroutineDispatcher = Handler(looper).asCoroutineDispatcher()
	private val supervisorContext = supervisorDispatcher + SupervisorJob()
	private val supervisorScope = CoroutineScope(supervisorContext)

	private val _sessionLock = ReentrantLock(true)
	private val _sessionMap = mutableMapOf<String, PlaybackSession>()
	private val _sessionListeners = mutableListOf<(PlaybackSession?) -> Unit>()
	private var _session: PlaybackSession? = null

	override fun getSession(id: String?): PlaybackSession? {
		return _sessionLock.withLock {
			if (id == null) _session else _sessionMap[id].also { check(it?.id == id) }
		}
	}

	override fun observeCurrentSession(): Flow<PlaybackSession?> {
		return callbackFlow {

			val listener: (PlaybackSession?) -> Unit = {
				supervisorScope.launch(supervisorDispatcher.immediate) { send(_session) }
			}

			val session = _sessionLock.withLock {
				_sessionListeners.add(listener)
				_session
			}

			send(session)

			awaitClose {
				_sessionLock.withLock { _sessionListeners.remove(listener) }
			}
		}
	}

	fun setCurrentSession(session: PlaybackSession?) {
		_sessionLock.withLock {
			_session = session
		}
		supervisorScope.launch(supervisorDispatcher) {
			val listeners = _sessionLock.withLock { ArrayList(_sessionListeners) }
			listeners.forEach { it(session) }
		}
	}
}
