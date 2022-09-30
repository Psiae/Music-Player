package com.flammky.common.kotlin.generic

fun interface Changed<T: Any> {
	fun onChanged(old: T?, new: T?)
}

fun interface ChangedNotNull<T: Any> {
	fun onChanged(old: T?, new: T)
}

fun interface ChangedNonNull<T: Any> {
	fun onChanged(old: T, new: T)
}
