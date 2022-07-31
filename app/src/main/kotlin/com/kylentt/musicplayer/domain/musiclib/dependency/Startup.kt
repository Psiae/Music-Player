package com.kylentt.musicplayer.domain.musiclib.dependency

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary

class MusicLibraryInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		MusicLibrary.localAgent.connect()
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf(AgentInitializer::class.java)
	}
}

class AgentInitializer : Initializer<Unit> {

	override fun create(context: Context): Unit {
		val provider = ValueProvider.args(
			context.applicationContext as Application,
			CoroutineDispatchers.DEFAULT
		)

		MusicLibrary.localAgent.dependency.provide(*provider)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
