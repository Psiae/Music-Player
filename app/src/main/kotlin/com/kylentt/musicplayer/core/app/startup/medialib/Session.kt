package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.RealMediaLibraryAPI
import com.kylentt.musicplayer.medialib.session.LibrarySession

class LibrarySessionInitializer : Initializer<LibrarySession> {
	override fun create(context: Context): LibrarySession {
		val api = MediaLibrary.API
		val session = api.sessions.builder.buildSession { setId("DEBUG") }
		return session.also {
			api.sessions.manager.addSession(it)
			it.mediaController.connectService()
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf(ApiInitializer::class.java)
}
