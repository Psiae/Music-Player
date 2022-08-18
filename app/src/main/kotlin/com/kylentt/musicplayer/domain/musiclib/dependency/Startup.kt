package com.kylentt.musicplayer.domain.musiclib.dependency

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.core.MusicLibrary

class MusicLibStartup : Initializer<Unit> {

	override fun create(context: Context) {
		MusicLibrary.construct(context)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
