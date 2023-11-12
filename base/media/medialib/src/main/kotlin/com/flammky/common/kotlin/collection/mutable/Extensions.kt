package com.flammky.common.kotlin.collection.mutable

inline fun <T> MutableCollection<T>.forEachClear(lock: Any, crossinline action: (T) -> Unit) {
	synchronized(lock) {
		forEach(action)
		clear()
	}
}

inline fun <T> MutableCollection<T>.forEachClear(crossinline action: (T) -> Unit) {
	forEach(action)
	clear()
}

inline fun <T : () -> Unit> MutableCollection<T>.doEachClear(lock: Any) {
	forEachClear(lock) { it() }
}
