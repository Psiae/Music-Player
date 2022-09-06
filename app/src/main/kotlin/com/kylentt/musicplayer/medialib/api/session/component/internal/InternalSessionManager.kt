package com.kylentt.musicplayer.medialib.api.session.component.internal

import com.kylentt.musicplayer.common.generic.sync
import com.kylentt.musicplayer.medialib.api.session.component.SessionManager
import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext
import com.kylentt.musicplayer.medialib.session.LibrarySession
import timber.log.Timber

internal class InternalSessionManager(private val libraryContext: MediaLibraryContext) : SessionManager {
	private val lock = Any()
	private val sessions = mutableListOf<LibrarySession>()

	override fun addSession(session: LibrarySession) {
		Timber.d("InternalSessionManager, ${session.id} added")
		sessions.sync(lock) { if (!contains(session)) add(session) }
	}

	override fun removeSession(session: LibrarySession) {
		sessions.sync(lock) { remove(session) }
	}

	override fun findSessionById(id: String): LibrarySession? {
		Timber.d("InternalSessionManager, findSessionById: $id currentSize(${sessions.size})")
		return sessions.firstOrNull { it.id == id }
	}
}
