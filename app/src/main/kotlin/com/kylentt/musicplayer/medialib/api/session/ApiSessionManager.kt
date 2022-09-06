package com.kylentt.musicplayer.medialib.api.session

import com.kylentt.musicplayer.medialib.session.LibrarySession
import com.kylentt.musicplayer.medialib.session.internal.LibrarySessionBuilder
import com.kylentt.musicplayer.medialib.session.internal.LibrarySessionManager

class ApiSessionManager internal constructor(private val parent: LibrarySessionManager) {
	fun addSession(session: LibrarySession) = parent.addSession(session)
	fun removeSession(session: LibrarySession) = parent.removeSession(session)

	fun get(id: String) = parent.get(id)

	internal val sessionBuilder: LibrarySessionBuilder = parent.sessionBuilder
}
