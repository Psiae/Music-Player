package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer

class MediaLibraryInitializer : Initializer<Unit> {

	override fun create(context: Context): Unit {
		// Touch
		com.flammky.android.medialib.temp.MediaLibrary.API
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>().apply {
			add(LibrarySessionInitializer::class.java)
			add(ApiInitializer::class.java)
		}
	}
}
