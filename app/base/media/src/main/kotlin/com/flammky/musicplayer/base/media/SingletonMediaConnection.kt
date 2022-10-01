package com.flammky.musicplayer.base.media

import com.flammky.kotlin.common.sync.sync
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

class SingletonMediaConnection private constructor(): MediaConnection {

	private val lock = Any()

	private var _state: MediaConnection.State = MediaConnection.State.NONE
		get() {
			check(Thread.holdsLock(lock))
			return field
		}
		set(value) {
			check(Thread.holdsLock(lock))
			field = value
		}

	override val state: MediaConnection.State
		get() = sync(lock) { _state }

	override val currentSession: MediaConnection.Session?
		get() {
			if (state != MediaConnection.State.CONNECTED) return null
			return TODO()
		}

	override suspend fun connectAsync(): Deferred<MediaConnection.ConnectionResult> {
		TODO("Not yet implemented")
	}

	override suspend fun observeConnection(): StateFlow<MediaConnection.ConnectionInfo> {
		TODO("Not yet implemented")
	}

	companion object {
		val instance = SingletonMediaConnection()
	}
}
