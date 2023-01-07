package com.flammky.musicplayer.base

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher

object Playback {

	val LOOPER: Looper by lazy {
		HandlerThread("Playback-looper").apply { start() }.looper
	}
	val DISPATCHER: CoroutineDispatcher by lazy {
		Handler(LOOPER).asCoroutineDispatcher("Playback-looper-coroutineDispatcher")
	}
}
