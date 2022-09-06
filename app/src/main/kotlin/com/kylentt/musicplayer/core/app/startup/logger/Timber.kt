package com.kylentt.musicplayer.core.app.startup.logger

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
	override fun create(context: Context) = Timber.plant(Timber.DebugTree())
	override fun dependencies(): MutableList<Class<out Initializer<*>>> = ArrayList(0)
}
