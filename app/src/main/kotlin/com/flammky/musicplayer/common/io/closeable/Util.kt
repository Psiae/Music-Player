package com.flammky.musicplayer.common.io.closeable

import java.io.Closeable

object CloseableUtil {

	inline fun <C: Closeable>C.applyClose(apply: C.() -> Unit): C = use {
		apply()
		this
	}
}
