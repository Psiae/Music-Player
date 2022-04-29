package com.kylentt.mediaplayer.app

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import timber.log.Timber

class AppInitializer : Initializer<Unit> {

  override fun create(context: Context) {
    require(context is Application)
    AppDelegate.provides(context)
  }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> {
    val dependencies = mutableListOf<Class<out Initializer<*>>>()
    if (BuildConfig.DEBUG) dependencies.add(DebugInitializer::class.java)
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
