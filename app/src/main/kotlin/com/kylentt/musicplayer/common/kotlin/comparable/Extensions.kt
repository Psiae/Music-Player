package com.kylentt.musicplayer.common.kotlin.comparable

fun <T : Comparable<T>> T.clamp(min: T, max: T) = clampValue(this, min, max)
fun <T : Comparable<T>> clampValue(value: T, min: T, max: T) = value.coerceIn(min, max)
