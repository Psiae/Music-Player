package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.session.LibrarySession

class LibrarySessionInitializer : Initializer<LibrarySession> {

	override fun create(context: Context): LibrarySession {
		requireNotNull(MediaLibraryAPI.current)
		val api = MediaLibraryAPI.current!!
		return LibrarySession.Builder("DEBUG").build(api).also { api.sessionManager.addSession(it) }
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf(ApiInitializer::class.java)

}
