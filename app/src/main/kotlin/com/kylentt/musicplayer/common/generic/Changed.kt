package com.kylentt.musicplayer.common.generic

fun interface Changed<T> {
	fun onChanged(old: T?, new: T?)
}

fun interface ChangedNotNull<T> {
	fun onChanged(old: T?, new: T)
}

fun interface ChangedNonNull<T> {
	fun onChanged(old: T, new: T)
}
