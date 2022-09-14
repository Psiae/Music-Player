package com.flammky.musicplayer.core.app.startup

import android.content.Context
import androidx.startup.Initializer
import com.flammky.android.app.AppDelegate

class AppDelegateInitializer : Initializer<Unit> {

	override fun create(context: Context): Unit {
		AppDelegate provides context
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = ArrayList(0)
}
