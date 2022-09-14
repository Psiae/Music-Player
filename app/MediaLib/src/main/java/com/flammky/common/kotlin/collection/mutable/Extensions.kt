package com.flammky.common.kotlin.collection.mutable

inline fun <T> MutableCollection<T>.forEachClear(lock: Any?, crossinline action: (T) -> Unit) {
	val doAction = { forEach(action) }
	if (lock != null) synchronized(lock) { doAction() } else doAction()
	clear()
}

inline fun <T> MutableCollection<T>.forEachClear(crossinline action: (T) -> Unit) {
	forEachClear(null, action)
}

inline fun <T : () -> Unit> MutableCollection<T>.doEachClear(lock: Any? = null) {
	forEachClear(lock) { it() }
}
