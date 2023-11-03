package dev.dexsr.klio.android.base

import android.os.Looper

fun inMainLooper(): Boolean {
    return Looper.myLooper()
        ?.let { looper ->
            looper == Looper.getMainLooper()
        } == true
}

inline fun checkInMainLooper() = checkInMainLooper(
    lazyMessage = {
        "Invalid Thread Access, expected Main, " +
                "current=${Thread.currentThread().name};looper=${Looper.myLooper()}"
    }
)

inline fun checkInMainLooper(
    lazyMessage: () -> Any
) = check(
    value = inMainLooper(),
    lazyMessage = lazyMessage
)
