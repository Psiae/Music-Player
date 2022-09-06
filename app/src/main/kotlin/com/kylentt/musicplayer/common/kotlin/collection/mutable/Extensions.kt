package com.kylentt.musicplayer.common.kotlin.collection.mutable

inline fun <T> MutableCollection<T>.forEachClear(lock: Any?, crossinline action: (T) -> Unit) {
	val doAction = { forEach(action) }
	if (lock != null) synchronized(lock) { doAction() } else doAction()
	clear()
}

fun <T> MutableCollection<T>.forEachClear(action: (T) -> Unit) {
	forEachClear(null, action)
}

fun <T : () -> Unit> MutableCollection<T>.doEachClear(lock: Any? = null) {
	forEachClear(lock) { it() }
}
