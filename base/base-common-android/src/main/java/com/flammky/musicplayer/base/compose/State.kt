package com.flammky.musicplayer.base.compose

import androidx.compose.runtime.MutableState

fun <T> MutableState<T>.rewrite(block: (T) -> T): MutableState<T> = apply { value = block(value) }
