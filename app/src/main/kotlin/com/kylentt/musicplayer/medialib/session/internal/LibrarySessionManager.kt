package com.kylentt.musicplayer.medialib.session.internal

import com.kylentt.musicplayer.common.generic.sync
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.session.LibrarySession


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
