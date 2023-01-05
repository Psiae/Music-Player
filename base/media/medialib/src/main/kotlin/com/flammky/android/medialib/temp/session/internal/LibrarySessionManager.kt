package com.flammky.android.medialib.temp.session.internal

import com.flammky.musicplayer.core.common.sync
import com.flammky.android.medialib.temp.internal.MediaLibraryContext
import com.flammky.android.medialib.temp.session.LibrarySession


internal class LibrarySessionManager(context: MediaLibraryContext) {

	private val sessionLock = Any()
	private val sessions: MutableList<LibrarySession> = mutableListOf()

	fun addSession(session: LibrarySession) {
		sessions.sync(sessionLock) { if (!contains(session)) add(session) }
	}

	fun removeSession(session: LibrarySession) {
		sessions.sync(sessionLock) { remove(session) }
	}

	fun get(id: String): LibrarySession? = sessions.sync(sessionLock) { firstOrNull { it.id == id } }

	val sessionBuilder: LibrarySessionBuilder = LibrarySessionBuilder(context)
}
