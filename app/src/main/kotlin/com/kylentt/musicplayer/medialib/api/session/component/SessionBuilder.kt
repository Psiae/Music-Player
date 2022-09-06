package com.kylentt.musicplayer.medialib.api.session.component

import com.kylentt.musicplayer.medialib.session.LibrarySession

interface SessionBuilder {
	fun buildSession(fill: LibrarySession.Builder.() -> Unit): LibrarySession
}
