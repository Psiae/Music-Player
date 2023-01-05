package com.flammky.musicplayer.base

import android.content.Context
import androidx.startup.Initializer
import com.flammky.musicplayer.core.CoreDebugInitializer
import com.flammky.musicplayer.core.CoreInitializer
import com.flammky.musicplayer.core.common.atomic

class BaseModuleInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		check(C.incrementAndGet() == 0) {
			"BaseModuleInitializer was called multiple times"
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> {
		return mutableListOf<Class<out Initializer<*>>>()
			.apply {
				add(CoreInitializer::class.java)
				if (BuildConfig.DEBUG) {
					add(BaseModuleDebugInitializer::class.java)
				}
			}
	}

	companion object {
		private val C = atomic(0)
	}
}

private class BaseModuleDebugInitializer : Initializer<Unit> {

	override fun create(context: Context) {
		check(BuildConfig.DEBUG) {
			"BaseModuleDebugInitializer was called on non-debug build config"
		}
		check(C.incrementAndGet() == 0) {
			"BaseModuleDebugInitializer was called multiple times"
		}
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> =
		mutableListOf(CoreDebugInitializer::class.java)

	companion object {
		private val C = atomic(0)
	}
}
