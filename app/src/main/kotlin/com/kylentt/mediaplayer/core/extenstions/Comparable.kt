package com.kylentt.mediaplayer.core.extenstions

import androidx.compose.ui.unit.Dp
import kotlin.time.Duration

fun <T : Comparable<T>> clampValue(value: T, min: T, max: T) = value.coerceIn(min, max)
fun <T : Comparable<T>> T.clamp(min: T, max: T) = clampValue(this, min, max)
