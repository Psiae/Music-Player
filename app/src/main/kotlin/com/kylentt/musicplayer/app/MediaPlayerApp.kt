package com.kylentt.musicplayer.app

import android.app.Application
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.musicplayer.app.AppInitializer.Companion.initialize
import com.kylentt.musicplayer.app.AppProxy.provideWrapper
import dagger.hilt.android.HiltAndroidApp
import leakcanary.LeakCanary
import timber.log.Timber

@HiltAndroidApp
internal class MediaPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize()
    }
}

internal class AppInitializer private constructor(
    private val app: Application
) {

    companion object {
        private lateinit var initializer: AppInitializer
        fun Application.initialize() = run {
            initializer = AppInitializer(this)
            initializer.init()
            initializer
        }
    }

    private fun init() {
        with(app) {
            if (BuildConfig.DEBUG) DebugImpl.initApp(this) else ReleaseImpl.initApp(this)
            BaseImpl.initApp(this)
        }
    }

    private object ReleaseImpl {
        fun initApp(base: Application) { /* Todo() */ }
    }

    private object DebugImpl {

        fun initApp(base: Application) {
            plantTimber()
            configureLeakCanary()
        }

        private fun plantTimber() {
            val debugTree = Timber.DebugTree()
            Timber.plant(debugTree).also { Timber.i("Timber Planted") }
        }

        private fun configureLeakCanary(isEnable: Boolean = BuildConfig.DEBUG) {
            LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnable)
            LeakCanary.showLeakDisplayActivityLauncherIcon(isEnable)
        }
    }

    private object BaseImpl {
        fun initApp(base: Application) {
            base.provideWrapper()
        }
    }
}


