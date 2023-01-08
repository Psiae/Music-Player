package com.flammky.musicplayer

import android.content.Context
import androidx.startup.Initializer
import com.flammky.android.app.AppDelegate
import com.flammky.musicplayer.base.BaseModuleInitializer
import com.flammky.musicplayer.core.app.startup.AppDelegateInitializer
import com.flammky.musicplayer.core.app.startup.medialib.MediaLibraryInitializer
import com.flammky.musicplayer.core.common.atomic

/**
 * Initializer Interface from [Initializer], specified in AndroidManifest.xml
 * @see [AppDelegate]
 * @author Flammky
 * @since 2022/04/30
 */

class AppInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		check(C.incrementAndGet() == 1) {
			"AppInitializer was called multiple time"
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>()
			.apply {
				if (BuildConfig.DEBUG) {
					add(AppDebugInitializer::class.java)
				}
				add(BaseModuleInitializer::class.java)
				add(AppDelegateInitializer::class.java)
				add(MediaLibraryInitializer::class.java)
			}
	}

	companion object {
		private val C = atomic(0)
	}
}

class AppDebugInitializer() : Initializer<Unit> {

	override fun create(context: Context) {
		check(C.incrementAndGet() == 1)
		check(BuildConfig.DEBUG) {
			"AppDebugInitializer was called on non-debug build config"
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>()
			.apply {
				add(BaseModuleInitializer::class.java)
			}
	}

	companion object {
		private val C = atomic(0)
	}
}
