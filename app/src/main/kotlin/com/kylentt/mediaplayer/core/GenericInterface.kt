package com.kylentt.mediaplayer.core

fun interface OnChanged<T> {
	fun onChanged(old: T?, new: T)
}
