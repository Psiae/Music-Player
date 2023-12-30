package com.flammky.musicplayer.core.common

inline fun <T : Any, R> T.sync(lock: Any = this, block: T.() -> R): R {
	return synchronized(lock) { block() }
}

inline fun <T : Any> T.sync(lock: Any = this) {
	return synchronized(lock) {}
}

inline fun <T: Any> T.syncApply(lock: Any = this, apply: T.() -> Unit): T {
	sync(lock, apply)
	return this
}
