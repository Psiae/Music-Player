package com.kylentt.disposed.musicplayer.app

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.disposed.musicplayer.ui.helper.AppToaster
import timber.log.Timber

class BaseInitializer : Initializer<Unit> {
  override fun create(context: Context) { /* TODO() WorkManager */ }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> {
    val depends = mutableListOf<Class<out Initializer<*>>>()
    if (BuildConfig.DEBUG) {
      depends.add(DebugInitializer::class.java)
    }
    depends.add(HelperInitializer::class.java)
    return depends
  }
}

class DebugInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    Timber.plant(Timber.DebugTree())
    Timber.d("Timber Planted")
  }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}

class HelperInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    require(context is Application)
    AppProxy.provideBase(context)
    AppToaster.provides(context)
    AppDelegate.provides(context)
  }

  override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()
}
