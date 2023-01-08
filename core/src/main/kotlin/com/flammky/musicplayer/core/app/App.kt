package com.flammky.musicplayer.core.app

import android.app.Application
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull

object App {
    private val SINGLETON = LazyConstructor<Application>()

    internal infix fun provide(application: Application) = SINGLETON.constructOrThrow(
        lazyValue = {
            application
        },
        lazyThrow = {
            error("Internal Module Error: App was already provided")
        }
    )

    fun get(): Application = SINGLETON.valueOrNull()
        ?: error("App was not provided, make sure CoreModuleInitializer was executed")
}