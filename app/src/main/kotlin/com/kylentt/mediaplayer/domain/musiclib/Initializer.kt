package com.kylentt.mediaplayer.domain.musiclib

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.app.dependency.AppModule

class MusicLibraryInitializer : Initializer<Unit> {

	override fun create(context: Context) = create(context as Application)

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

	private fun create(app: Application) {
		DependencyBundle()
			.provides(app, AppModule.provideAppDispatchers())
			.run { MusicLibrary.initialize(this) }
	}
}
