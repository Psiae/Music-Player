package com.kylentt.musicplayer.domain.helper

import android.os.Looper
import timber.log.Timber

object Preconditions {

  fun verifyMainThread(msg: String = "") {
    check(Looper.myLooper() == Looper.getMainLooper()) {
      Timber.wtf("Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}\n msg = $msg")
    }
  }
}
