package com.kylentt.musicplayer.common.throwable

fun Iterable<Throwable>.throwAll() = forEach { throw it }
