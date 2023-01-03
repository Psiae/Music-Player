package com.flammky.musicplayer.common.media.audio.meta_tag.logging

/**
 * Display as hex
 */
object Hex {
	/**
	 * Display as hex
	 *
	 * @param value
	 * @return
	 */
	fun asHex(value: Long): String {
		val `val` = java.lang.Long.toHexString(value)
		return if (`val`.length == 1) {
			"0x0$`val`"
		} else "0x$`val`"
	}

	fun asHex(value: Int): String {
		return "0x" + Integer.toHexString(value)
	}

	/**
	 * Display as hex
	 *
	 * @param value
	 * @return
	 */
	fun asHex(value: Byte): String {
		return "0x" + Integer.toHexString(value.toInt())
	}

	/**
	 * Display as integral and hex calue in brackets
	 *
	 * @param value
	 * @return
	 */
	fun asDecAndHex(value: Long): String {
		return value.toString() + " (" + asHex(value) + ")"
	}
}
