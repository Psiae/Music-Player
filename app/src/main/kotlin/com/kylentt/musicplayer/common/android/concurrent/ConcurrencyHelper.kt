package com.kylentt.musicplayer.common.android.concurrent

import android.os.Looper
import java.lang.Thread.currentThread

object ConcurrencyHelper {

	val Thread.isMain get() = this == Looper.getMainLooper().thread

	fun isMainThread() = currentThread().isMain

	fun checkMainThread() {
		checkMainThread {
			"Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}"
		}
	}

	inline fun checkMainThread(lazyMessage: () -> Any) = check(isMainThread(), lazyMessage = lazyMessage)
}
