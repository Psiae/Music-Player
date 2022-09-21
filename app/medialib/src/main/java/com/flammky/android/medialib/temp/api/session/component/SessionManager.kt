package com.flammky.android.medialib.temp.api.session.component

import com.flammky.android.medialib.temp.session.LibrarySession

interface SessionManager {
	fun addSession(session: LibrarySession)
	fun removeSession(session: LibrarySession)

	fun findSessionById(id: String): LibrarySession?
}
