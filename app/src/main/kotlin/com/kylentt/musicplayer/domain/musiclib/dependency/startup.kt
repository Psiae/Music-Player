package com.kylentt.musicplayer.domain.musiclib.dependency

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.core.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.interactor.Agent

class MusicLibraryInitializer : Initializer<Unit> {

	override fun create(context: Context) = Unit

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf(AgentInitializer::class.java)
	}
}

class AgentInitializer : Initializer<Agent> {

	override fun create(context: Context): Agent {
		val provider = ValueProvider.args(
			context.applicationContext as Application,
			CoroutineDispatchers.DEFAULT
		)

		return MusicLibrary.localAgent
			.apply {
				injector.addProvider(*provider)
				initializer.initialize()
			}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
