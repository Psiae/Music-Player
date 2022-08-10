package com.kylentt.musicplayer.core.app.startup

import android.content.Context
import androidx.startup.Initializer
import com.kylentt.musicplayer.BuildConfig
import com.kylentt.musicplayer.core.app.AppDelegate

/**
 * Initializer Interface from [Initializer], specified in AndroidManifest.xml
 * @see [AppDelegate]
 * @author Kylentt
 * @since 2022/04/30
 */

class AppInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		AppDelegate.deviceManager
	}
	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		with(mutableListOf<Class<out Initializer<*>>>()) {
			if (BuildConfig.DEBUG) add(TimberInitializer::class.java)
			add(AppDelegateInitializer::class.java)
			add(MusicLibraryInitializer::class.java)
			return this
		}
	}
}
