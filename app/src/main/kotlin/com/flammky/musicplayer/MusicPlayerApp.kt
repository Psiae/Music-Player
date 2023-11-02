package com.flammky.musicplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.koin.core.context.startKoin

@HiltAndroidApp
class MusicPlayerApp : Application() {

	override fun onCreate() {
		super.onCreate()
		startKoin {
			// TODO
		}
	}
}
