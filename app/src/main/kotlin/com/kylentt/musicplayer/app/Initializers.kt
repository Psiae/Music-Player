package com.kylentt.musicplayer.app

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.kylentt.mediaplayer.BuildConfig
import leakcanary.LeakCanary
import timber.log.Timber
import javax.inject.Inject

class BaseInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AppProxy.provideBase(context as Application)
        /* TODO() WorkManager */
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        val depends = mutableListOf<Class<out Initializer<*>>>()
        if (BuildConfig.DEBUG) depends.add(DebugInitializer::class.java)
        return depends
    }
}

class DebugInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(Timber.DebugTree())
        Timber.d("Timber Planted")
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> { return mutableListOf() }
}