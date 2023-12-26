package com.flammky.musicplayer.base

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher

object Playback {
	val THREAD: Thread by lazy {
		HandlerThread("Playback-looper").apply { start() }
	}
	val LOOPER: Looper by lazy {
		(THREAD as HandlerThread).looper
	}
	val DISPATCHER: CoroutineDispatcher by lazy {
		Handler(LOOPER).asCoroutineDispatcher("Playback-looper-coroutineDispatcher")
	}
}
