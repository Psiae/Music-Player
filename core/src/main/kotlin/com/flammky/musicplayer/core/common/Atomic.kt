package com.flammky.musicplayer.core.common

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

fun <T> atomic(v: T): AtomicReference<T> = AtomicReference(v)
fun atomic(v: Int): AtomicInteger = AtomicInteger(v)
fun atomic(v: Boolean): AtomicBoolean = AtomicBoolean(v)
fun atomic(v: Long): AtomicLong = AtomicLong(v)
