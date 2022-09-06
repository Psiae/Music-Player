package com.kylentt.musicplayer.medialib.session

import com.kylentt.musicplayer.medialib.internal.MediaLibraryContext

class SessionContext private constructor(private val libContext: MediaLibraryContext) {


	class Builder internal constructor(private val context: MediaLibraryContext) {
		fun build(): SessionContext = SessionContext(context)
	}
}
