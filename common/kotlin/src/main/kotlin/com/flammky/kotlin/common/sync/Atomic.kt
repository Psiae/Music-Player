package com.flammky.kotlin.common.sync

import java.util.concurrent.atomic.AtomicReference

fun <T> atomic(v: T): AtomicReference<T> = AtomicReference(v)
