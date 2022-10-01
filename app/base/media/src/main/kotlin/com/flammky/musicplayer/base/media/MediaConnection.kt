package com.flammky.musicplayer.base.media

import com.flammky.android.medialib.player.Player
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

/**
 * Connector to our Media Services.
 */
interface MediaConnection {
	val state: State
	val currentSession: Session?

	suspend fun connectAsync(): Deferred<ConnectionResult>
	suspend fun observeConnection(): StateFlow<ConnectionInfo>

	/**
	 * Session representation within [MediaConnection]
	 */
	interface Session {
		val player: Player
	}

	enum class State {
		NONE,
		CONNECTED,
		DISCONNECTED
	}

	data class ConnectionInfo(
		val isConnected: Boolean,
		val currentSession: Session?
	) {
		companion object {
			val UNSET = ConnectionInfo(false, null)
		}
	}

	sealed interface ConnectionResult {
		object SUCCESS : ConnectionResult
		data class ERROR(val ex: Exception) : ConnectionResult
	}

	companion object {
		inline val MediaConnection.isConnected
			get() = state == State.CONNECTED
	}
}
