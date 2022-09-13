package com.flammky.android.medialib.temp.api.session.component

import com.flammky.android.medialib.temp.session.LibrarySession

interface SessionBuilder {
	fun buildSession(fill: LibrarySession.Builder.() -> Unit): LibrarySession
}
