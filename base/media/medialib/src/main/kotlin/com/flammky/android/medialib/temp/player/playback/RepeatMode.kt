package com.flammky.android.medialib.temp.player.playback

import androidx.media3.common.Player

sealed class RepeatMode : Comparable<Any> {

	/**
	 * Repeat on Current MediaItem scope,
	 * i.e: repeat currently playing MediaItem infinitely
	 */
	object CURRENT : RepeatMode() {
		override fun compareTo(other: Any): Int = when(other) {
			NONE, 0 -> 1
			CURRENT, 1 -> 0
			TIMELINE, 2 -> -1
			else -> -1
		}
	}

	/**
	 * Repeat on Current TimeLine scope,
	 * i.e: next of normally last seek-able MediaItem is the first one and vice-versa,
	 * i.e: repeat currently playing TimeLine or essentially playList infinitely
	 */
	object TIMELINE : RepeatMode() {
		override fun compareTo(other: Any): Int = when(other) {
			NONE, CURRENT, 0, 1 -> 1
			TIMELINE, 2 -> 0
			else -> -1
		}
	}

	/**
	 * No RepeatMode applied
	 */
	object NONE : RepeatMode() {
		override fun compareTo(other: Any): Int = when(other) {
			NONE, 0 -> 0
			CURRENT, TIMELINE, 1, 2 -> -1
			else -> -1
		}
	}

	companion object {
		inline val @Player.RepeatMode Int.asRepeatMode: RepeatMode
			get() = when(this) {
				Player.REPEAT_MODE_OFF -> NONE
				Player.REPEAT_MODE_ONE -> CURRENT
				Player.REPEAT_MODE_ALL -> TIMELINE
				else -> throw IllegalArgumentException("Trying to cast invalid $this as ${RepeatMode::class}")
			}

		inline val RepeatMode.toRepeatModeInt: Int
			get() = when(this) {
				NONE -> Player.REPEAT_MODE_OFF
				CURRENT -> Player.REPEAT_MODE_ONE
				TIMELINE -> Player.REPEAT_MODE_ALL
			}
	}
}
