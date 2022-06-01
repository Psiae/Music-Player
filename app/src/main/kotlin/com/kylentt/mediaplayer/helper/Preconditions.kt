package com.kylentt.mediaplayer.helper

import android.os.Looper

@Suppress("NOTHING_TO_INLINE")
object Preconditions {

  @JvmStatic inline fun checkState(state: Boolean): Unit = check(state)

  @JvmStatic inline fun checkState(state: Boolean, msg: () -> Any): Unit = check(state, msg)

  @JvmStatic inline fun checkArgument(argument: Boolean): Unit = require(argument)

  @JvmStatic inline fun checkArgument(argument: Boolean, msg: () -> Any) = require(argument, msg)

  @JvmStatic inline fun checkMainThread() {
		checkMainThread { "Check Failed, Must be called from Main Thread" }
	}

  @JvmStatic inline fun checkMainThread(msg: () -> Any) {
    checkState(isMainThread()) {
      "Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}" +
        "\nmsg = ${msg()}"
    }
  }

  @JvmStatic inline fun checkNotMainThread() {
    checkNotMainThread {
      "Check Failed, Must not be called from Main Thread: ${Looper.getMainLooper()}"
    }
  }

  @JvmStatic inline fun checkNotMainThread(msg: () -> Any) {
    checkState(!isMainThread()) {
      "Invalid Thread, Expected: Not ${Looper.getMainLooper()}, caught: ${Looper.getMainLooper()}" +
        "\n msg = ${msg()}"
    }
  }

	inline fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
