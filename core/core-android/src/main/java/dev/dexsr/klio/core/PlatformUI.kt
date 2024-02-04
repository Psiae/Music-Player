package dev.dexsr.klio.core

import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

object AndroidUiFoundation : UiFoundation()

fun AndroidUiFoundation.isOnUiLooper(): Boolean {
    return Looper.myLooper()
        ?.let { looper -> looper == Looper.getMainLooper() }
        ?: false
}

val AndroidUiFoundation.MainDispatcher: MainCoroutineDispatcher
    get() = Dispatchers.Main