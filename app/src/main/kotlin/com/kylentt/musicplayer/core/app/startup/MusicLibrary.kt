package com.kylentt.musicplayer.core.app.startup

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.core.app.startup.medialib.MediaLibraryInitializer
import com.kylentt.musicplayer.domain.musiclib.dependency.MusicLibStartup

class MusicLibraryInitializer() : Initializer<Unit> {
	override fun create(context: Context) = Unit
	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf(MusicLibStartup::class.java, MediaLibraryInitializer::class.java)
	}
}
