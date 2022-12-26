package com.flammky.musicplayer.dump.mediaplayer.helper

import android.os.Looper

@Suppress("NOTHING_TO_INLINE")
object Preconditions {

	inline fun checkState(state: Boolean): Boolean {
		return checkState(state) { "CheckState Failed" }
	}

	inline fun checkState(state: Boolean, msg: () -> Any): Boolean {
		check(state, msg)
		return state
	}

	inline fun checkArgument(argument: Boolean): Boolean {
		return checkArgument(argument) { "CheckArgument Failed" }
	}

	inline fun checkArgument(argument: Boolean, msg: () -> Any): Boolean {
		require(argument, msg)
		return argument
	}

	inline fun checkMainThread(): Boolean {
		return checkMainThread { "Check Failed, Must be called from Main Thread" }
	}

	inline fun checkMainThread(msg: () -> Any): Boolean {
    return checkState(isMainThread()) {
      "Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}" +
        "\nmsg = ${msg()}"
    }
  }

	inline fun checkNotMainThread() {
    checkNotMainThread {
      "Check Failed, Must not be called from Main Thread: ${Looper.getMainLooper()}"
    }
  }

	inline fun checkNotMainThread(msg: () -> Any) {
    checkState(!isMainThread()) {
      "Invalid Thread, Expected: Not ${Looper.getMainLooper()}, caught: ${Looper.getMainLooper()}" +
        "\n msg = ${msg()}"
    }
  }

	inline fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
