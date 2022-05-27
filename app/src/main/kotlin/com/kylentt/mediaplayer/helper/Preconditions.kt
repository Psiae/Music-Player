package com.kylentt.mediaplayer.helper

import android.os.Looper

object Preconditions {

  @JvmStatic
  fun checkState(state: Boolean): Boolean {
    check(state)
    return true
  }

  @JvmStatic
  inline fun checkState(state: Boolean, msg: () -> Any): Boolean {
    check(state, msg)
    return true
  }

  @JvmStatic
  fun checkArgument(argument: Boolean): Boolean {
    require(argument)
    return true
  }

  @JvmStatic
  inline fun checkArgument(argument: Boolean, msg: () -> Any): Boolean {
    require(argument, msg)
    return true
  }


  @JvmStatic
  @Suppress("NOTHING_TO_INLINE")
  inline fun checkMainThread() {
    checkMainThread {
      "Check Failed, Must be called from Main Thread"
    }
  }

  @JvmStatic
  inline fun checkMainThread(msg: () -> Any) {
    checkState(Looper.myLooper() == Looper.getMainLooper()) {
      "Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}" +
        "\nmsg = ${msg()}"
    }
  }

  @Suppress("NOTHING_TO_INLINE")
  @JvmStatic
  inline fun checkNotMainThread() {
    checkNotMainThread {
      "Check Failed, Must not be called from Main Thread: ${Looper.getMainLooper()}"
    }
  }

  @JvmStatic
  inline fun checkNotMainThread(msg: () -> Any) {
    checkState(Looper.myLooper() != Looper.getMainLooper()) {
      "Invalid Thread, Expected: Not ${Looper.getMainLooper()}, caught: ${Looper.getMainLooper()}" +
        "\n msg = ${msg()}"
    }
  }
}
