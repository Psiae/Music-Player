package com.flammky.android.common.io.closeable

import java.io.Closeable

object CloseableUtil {

	inline fun <C: Closeable, R> C.applyUse(apply: C.() -> R): R = use(apply)
}
