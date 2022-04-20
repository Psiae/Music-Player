package com.kylentt.mediaplayer.helper

import android.os.Looper
import timber.log.Timber

object Preconditions {

  fun verifyMainThread(msg: String = "") {
    check(Looper.myLooper() == Looper.getMainLooper()) {
      "Invalid Thread, Expected: ${Looper.getMainLooper()}, Caught: ${Looper.myLooper()}" +
        "\nmsg = $msg"
    }
  }
}
