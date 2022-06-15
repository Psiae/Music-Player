package com.kylentt.mediaplayer.helper

import android.os.Looper

@Suppress("NOTHING_TO_INLINE")
object Preconditions {

  @JvmStatic inline fun checkState(state: Boolean): Boolean {
		checkState(state) {
			"CheckState Failed"
		}
		return state
	}

  @JvmStatic inline fun checkState(state: Boolean, msg: () -> Any): Boolean {
		check(state, msg)
		return state
	}

  @JvmStatic inline fun checkArgument(argument: Boolean): Boolean {
		checkArgument(argument) {
			"CheckArgument Failed"
		}
		return argument
	}

  @JvmStatic inline fun checkArgument(argument: Boolean, msg: () -> Any): Boolean {
		require(argument, msg)
		return argument
	}

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

	@JvmStatic inline fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
