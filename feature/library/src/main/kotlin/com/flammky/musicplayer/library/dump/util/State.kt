package com.flammky.musicplayer.library.dump.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State

// is explicit read like this better ?
internal fun <T> State<T>.read(): T {
	return value
}

// is explicit write like this better ?
internal fun <T> MutableState<T>.overwrite(value: T) {
	this.value = value
}

// is explicit write like this better ?
internal inline fun <T> MutableState<T>.rewrite(block: (T) -> T): T {
	this.value = block(this.value)
	return this.value
}
