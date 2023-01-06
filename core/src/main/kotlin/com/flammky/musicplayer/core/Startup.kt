package com.flammky.musicplayer.core

import android.content.Context
import androidx.startup.Initializer
import com.flammky.musicplayer.core.common.atomic
import timber.log.Timber

class CoreInitializer(): Initializer<Unit> {

    override fun create(context: Context) {
        check(C.incrementAndGet() == 1) {
            "CoreInitializer was called multiple times"
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf<Class<out Initializer<*>>>().apply {
            if (BuildConfig.DEBUG) {
                add(CoreDebugInitializer::class.java)
            }
        }
    }

    companion object {
        private val C = atomic(0)
    }
}

class CoreDebugInitializer(): Initializer<Unit> {

    override fun create(context: Context) {
        check(BuildConfig.DEBUG) {
            "CoreDebugInitializer was called on non-debug BuildConfig"
        }
        check(C.incrementAndGet() == 1) {
            "CoreDebugInitializer was called multiple times"
        }
        Timber.plant(Timber.DebugTree())
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

    companion object {
        private val C = atomic(0)
    }
}