package dev.flammky.compose_components.core

import android.os.Looper

internal fun inMainLooper(): Boolean {
    return Looper.myLooper()
        ?.let { myLooper ->
            myLooper == Looper.getMainLooper()
        }
        ?: false
}