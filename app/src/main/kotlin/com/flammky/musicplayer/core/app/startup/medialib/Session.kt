package com.flammky.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer

class LibrarySessionInitializer : Initializer<com.flammky.android.medialib.temp.session.LibrarySession> {
	override fun create(context: Context): com.flammky.android.medialib.temp.session.LibrarySession {
		val api = com.flammky.android.medialib.temp.MediaLibrary.API
		val session = api.sessions.builder.buildSession { setId("DEBUG") }
		return session.also {
			api.sessions.manager.addSession(it)
			it.mediaController.connectService()
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf(
        ApiInitializer::class.java)
}
