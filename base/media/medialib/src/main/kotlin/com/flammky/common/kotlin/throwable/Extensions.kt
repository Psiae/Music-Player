package com.flammky.common.kotlin.throwable

inline fun Iterable<Throwable>.throwAll() = forEach { throw it }
