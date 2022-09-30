package com.flammky.kotlin.common.sync

inline fun <T : Any, R> T.sync(lock: Any = this, block: T.() -> R): R {
	return synchronized(lock) { block() }
}

inline fun <T: Any> T.syncApply(lock: Any = this, apply: T.() -> Unit): T {
	sync(lock, apply)
	return this
}
