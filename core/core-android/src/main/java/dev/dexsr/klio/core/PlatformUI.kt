package dev.dexsr.klio.core

import android.os.Looper

object AndroidUiFoundation : UiFoundation()

fun AndroidUiFoundation.isOnUiThread(): Boolean {
    return Looper.myLooper()
        ?.let { looper -> looper == Looper.getMainLooper() }
        ?: false
}