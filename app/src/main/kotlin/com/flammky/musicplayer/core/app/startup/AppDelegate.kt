package com.flammky.musicplayer.core.app.startup

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.flammky.android.app.AppDelegate
import com.flammky.musicplayer.activity.ActivityWatcher

class AppDelegateInitializer : Initializer<Unit> {

	override fun create(context: Context): Unit {
		AppDelegate provides context
		ActivityWatcher provides context as Application
	}

	override fun dependencies(): MutableList<Class<out Initializer<*>>> = ArrayList(0)
}
