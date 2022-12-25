package com.flammky.musicplayer.app

import android.app.Application
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull

object ApplicationDelegate {
	private val constructor = LazyConstructor<Application>()

	infix fun provides(app: Application) = constructor.construct { app }

	fun get(): Application = constructor.valueOrNull()
		?: error("ApplicationDelegate was not provided")
}
