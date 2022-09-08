package com.kylentt.musicplayer.core.app.startup.medialib

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI

class MediaLibraryInitializer : Initializer<Unit> {

	override fun create(context: Context): Unit {
		requireNotNull(MediaLibraryAPI.current)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>().apply {
			add(LibrarySessionInitializer::class.java)
			add(ApiInitializer::class.java)
		}
	}
}
