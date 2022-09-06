package com.kylentt.musicplayer.medialib.api.session.component

import com.kylentt.musicplayer.medialib.session.LibrarySession

interface SessionManager {
	fun addSession(session: LibrarySession)
	fun removeSession(session: LibrarySession)

	fun findSessionById(id: String): LibrarySession?
}
