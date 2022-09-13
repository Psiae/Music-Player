package com.flammky.common.kotlin.throwable

fun Iterable<Throwable>.throwAll() = forEach { throw it }
