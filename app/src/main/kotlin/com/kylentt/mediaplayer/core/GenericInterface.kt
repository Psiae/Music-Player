package com.kylentt.mediaplayer.core

fun interface OnChanged<T> {
	fun onChanged(old: T?, new: T?)
}

fun interface OnChangedNotNull<T> {
	fun onChanged(old: T?, new: T)
}

fun interface OnChangedNonNull<T> {
	fun onChanged(old: T, new: T)
}
