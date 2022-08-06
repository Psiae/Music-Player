package com.kylentt.musicplayer.domain.musiclib.util

inline fun <T> withEach(iterable: Iterable<T>, action: (T) -> Unit) = iterable.forEach(action)
