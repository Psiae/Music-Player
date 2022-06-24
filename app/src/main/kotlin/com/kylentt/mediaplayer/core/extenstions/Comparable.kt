package com.kylentt.mediaplayer.core.extenstions

import androidx.compose.ui.unit.Dp
import kotlin.time.Duration

fun <T : Comparable<T>> clamp(value: T, min: T, max: T) = value.coerceIn(min, max)

fun Byte.clamp(min: Byte, max: Byte) = clamp(this, min, max)
fun Short.clamp(min: Short, max: Short) = clamp(this, min, max)
fun Int.clamp(min: Int, max: Int) = clamp(this, min, max)
fun Long.clamp(min: Long, max: Long) = clamp(this, min, max)

fun Double.clamp(min: Double, max: Double) = clamp(this, min, max)
fun Float.clamp(min: Float, max: Float) = clamp(this, min, max)

fun Duration.clamp(min: Duration, max: Duration) = clamp(this, min, max)

fun Dp.clamp(min: Dp, max: Dp) = clamp(this, min, max)


