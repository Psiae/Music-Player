package com.kylentt.mediaplayer.core.util

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

sealed class STATE() {
    class STATE_CHECKING() : STATE()
}

fun Any?.isNull() = this == null

fun Int?.orInv() = this ?: -1
fun Long?.orInv() = this ?: -1L

fun Int?.orZero() = this ?: 0
fun Long?.orZero() = this ?: 0L

fun Long.removeSuffix(suffix: String): Long {
    val s = this.toString()
    return if (s.endsWith(suffix)) s.removeSuffix(suffix).toLong() else this
}

fun <T> T?.orDefault(default: T) = this ?: default
