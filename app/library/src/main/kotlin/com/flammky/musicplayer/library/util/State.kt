package com.flammky.musicplayer.library.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State

// is explicit read like this better ?
@Suppress("NOTHING_TO_INLINE")
inline fun <T> State<T>.read(): T {
	return value
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
inline fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

// is explicit write like this better ?
@Suppress("NOTHING_TO_INLINE")
inline fun <T> MutableState<T>.rewrite(block: (T) -> T) {
	this.value = block(this.value)
}
