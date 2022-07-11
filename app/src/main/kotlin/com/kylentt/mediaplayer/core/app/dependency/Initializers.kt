package com.kylentt.mediaplayer.core.app.dependency

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.core.app.delegates.AppDelegate
import com.kylentt.musicplayer.domain.musiclib.dependency.MusicLibraryInitializer
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import timber.log.Timber

/**
 * Initializer Interface from [Initializer], specified in AndroidManifest.xml
 * @see [AppDelegate]
 * @author Kylentt
 * @since 2022/04/30
 */

class AppInitializer : Initializer<Unit> {

	override fun create(context: Context) = Unit

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		val dependencies = mutableListOf<Class<out Initializer<*>>>()
		with(dependencies) {
			if (BuildConfig.DEBUG) add(DebugInitializer::class.java)
			add(ApplicationInitializer::class.java)
			add(MusicLibraryInitializer::class.java)
		}

		return dependencies
	}
}

class DebugInitializer : Initializer<Unit> {

  override fun create(context: Context) {
    plantTimber()
  }

  private fun plantTimber() {
    Timber.plant(Timber.DebugTree())
    Timber.i("Timber Planted")
  }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
class ApplicationInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		AppDelegate.provides(context)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
