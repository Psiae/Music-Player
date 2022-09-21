package com.flammky.musicplayer.common.media.audio.meta_tag.utils

object PrimitiveUtils {
	fun safeLongToInt(l: Long): Int {
		require(!(l < Int.MIN_VALUE || l > Int.MAX_VALUE)) { "$l cannot be cast to int without changing its value." }
		return l.toInt()
	}
}
