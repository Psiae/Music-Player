package com.kylentt.mediaplayer.app.dependency

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.domain.musiclib.MusicLibraryInitializer
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import timber.log.Timber

/**
 * Initializer Interface from [Initializer], specified in AndroidManifest.xml
 * @see [AppDelegate]
 * @author Kylentt
 * @since 2022/04/30
 */

class AppInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		checkArgument(context is Application) {
			"AppInitializer Did Not Provide Application as Context"
		}
		create(context as Application)
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		val dependencies = mutableListOf<Class<out Initializer<*>>>()
		if (BuildConfig.DEBUG) dependencies.add(DebugInitializer::class.java)
		return dependencies
	}

	private fun create(app: Application) {
		AppDelegate provides app
		MusicLibraryInitializer().create(app)
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
