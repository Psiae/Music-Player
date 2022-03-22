package com.kylentt.mediaplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MediaPlayerApp : Application() {

    private val initializer = AppInitializer(this)

    override fun onCreate() {
        super.onCreate()
        initializer.init()
    }
}

class AppInitializer(private val app: Application) {

    fun init() {
        if (app is MediaPlayerApp) {
            if (BuildConfig.DEBUG) {
                plantTimber()
            }
        }
    }

    private fun plantTimber() {
        Timber.plant(Timber.DebugTree())
        Timber.i("Timber Planted")
    }

}