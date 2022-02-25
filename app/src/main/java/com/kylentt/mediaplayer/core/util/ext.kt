package com.kylentt.mediaplayer.core.util

fun Any?.isNull() = this == null

fun Int?.orInv() = this ?: -1
fun Long?.orInv() = this ?: -1L

fun Int?.orZero() = this ?: 0
fun Long?.orZero() = this ?: 0L



fun <T> T?.orDefault(default: T): T {
    return this ?: default
}