package com.kylentt.mediaplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MediaPlayerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (false) {
            Timber.plant(Timber.DebugTree())
            Timber.i("Timber Planted")
        }
    }
}