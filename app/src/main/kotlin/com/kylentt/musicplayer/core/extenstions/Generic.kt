package com.kylentt.musicplayer.core.extenstions

inline fun <T : Any, R> T.sync(lock: Any = this, block: T.() -> R): R {
	return synchronized(lock) { block() }
}
