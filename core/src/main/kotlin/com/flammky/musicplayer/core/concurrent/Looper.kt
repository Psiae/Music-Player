package com.flammky.musicplayer.core.concurrent

import android.os.Looper

fun inMainLooper(): Boolean {
    return Looper.myLooper()
        ?.let { looper ->
            looper == Looper.getMainLooper()
        }
        ?: false
}