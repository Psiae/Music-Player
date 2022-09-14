package com.flammky.musicplayer.domain.musiclib.dependency

import android.content.Context
import androidx.startup.Initializer
import com.flammky.musicplayer.domain.musiclib.core.MusicLibrary

class MusicLibStartup : Initializer<Unit> {

	override fun create(context: Context) {
		MusicLibrary.construct(context)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
