package com.flammky.common.kotlin.io

import java.io.Closeable

inline fun <C: Closeable, R> C.applyClose(apply: C.() -> R): R = use { apply() }