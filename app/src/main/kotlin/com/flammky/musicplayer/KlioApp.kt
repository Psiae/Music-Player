package com.flammky.musicplayer

import android.app.Application
import com.flammky.kotlin.common.lazy.LazyConstructor
import dagger.hilt.android.HiltAndroidApp
import org.koin.core.context.startKoin

@HiltAndroidApp
class KlioApp : Application() {

	override fun onCreate() {
		super.onCreate()
		Companion.provide(this)
		startKoin {
			// TODO
		}
	}

	companion object {
		private val INSTANCE = LazyConstructor<KlioApp>()

		fun provide(instance: KlioApp) = INSTANCE.constructOrThrow({ instance }, { error("KliApp was already provided") })

		fun require(): KlioApp = INSTANCE.value
	}
}
