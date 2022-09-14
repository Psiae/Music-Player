package com.flammky.musicplayer.common.throwable

fun Iterable<Throwable>.throwAll() = forEach { throw it }
