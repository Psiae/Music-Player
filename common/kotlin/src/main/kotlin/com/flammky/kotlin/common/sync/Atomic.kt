package com.flammky.kotlin.common.sync

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

fun <T> atomic(v: T): AtomicReference<T> = AtomicReference(v)
fun atomic(v: Int): AtomicInteger = AtomicInteger(v)
