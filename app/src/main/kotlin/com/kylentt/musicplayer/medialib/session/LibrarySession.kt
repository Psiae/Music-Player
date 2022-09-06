package com.kylentt.musicplayer.medialib.session

import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.player.MediaController

class LibrarySession private constructor(private val context: SessionContext) {

	val id: String = context.id

	val mediaController: MediaController get() = context.controller

	class Builder(val id: String) {
		internal fun build(context: SessionContext): LibrarySession {
			return LibrarySession(context)
		}

		fun build(api: MediaLibraryAPI): LibrarySession {
			val contextBuilder = SessionContext.Builder(id)
			return api.sessionManager.sessionBuilder.buildSession(this, contextBuilder.build())
		}
	}
}
