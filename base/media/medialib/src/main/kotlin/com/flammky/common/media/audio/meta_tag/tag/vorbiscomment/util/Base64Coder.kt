package com.flammky.musicplayer.common.media.audio.meta_tag.tag.vorbiscomment.util

import java.nio.charset.StandardCharsets

/**
 * Base64Coder
 */
object Base64Coder {
	// Mapping table from 6-bit nibbles to Base64 characters.
	private val map1 = CharArray(64)

	init {
		var i = 0
		run {
			var c = 'A'
			while (c <= 'Z') {
				map1[i++] = c
				c++
			}
		}
		run {
			var c = 'a'
			while (c <= 'z') {
				map1[i++] = c
				c++
			}
		}
		var c = '0'
		while (c <= '9') {
			map1[i++] = c
			c++
		}
		map1[i++] = '+'
		map1[i++] = '/'
	}

	// Mapping table from Base64 characters to 6-bit nibbles.
	private val map2 = ByteArray(128)

	init {
		for (i in map2.indices) {
			map2[i] = -1
		}
		for (i in 0..63) {
			map2[map1[i].code] = i.toByte()
		}
	}

	/**
	 * Encodes a string into Base64 format.
	 * No blanks or line breaks are inserted.
	 *
	 * @param s a String to be encoded.
	 * @return A String with the Base64 encoded data.
	 */
	fun encode(s: String): String {
		return String(encode(s.toByteArray(StandardCharsets.ISO_8859_1)))
	}

	/**
	 * Encodes a byte array into Base64 format.
	 * No blanks or line breaks are inserted.
	 *
	 * @param in an array containing the data bytes to be encoded.
	 * @return A character array with the Base64 encoded data.
	 */
	@JvmStatic
	fun encode(`in`: ByteArray): CharArray {
		val iLen = `in`.size
		val oDataLen = (iLen * 4 + 2) / 3 // output length without padding
		val oLen = (iLen + 2) / 3 * 4 // output length including padding
		val out = CharArray(oLen)
		var ip = 0
		var op = 0
		while (ip < iLen) {
			val i0 = `in`[ip++].toInt() and 0xff
			val i1 = if (ip < iLen) `in`[ip++].toInt() and 0xff else 0
			val i2 = if (ip < iLen) `in`[ip++].toInt() and 0xff else 0
			val o0 = i0 ushr 2
			val o1 = i0 and 3 shl 4 or (i1 ushr 4)
			val o2 = i1 and 0xf shl 2 or (i2 ushr 6)
			val o3 = i2 and 0x3F
			out[op++] = map1[o0]
			out[op++] = map1[o1]
			out[op] = if (op < oDataLen) map1[o2] else '='
			op++
			out[op] = if (op < oDataLen) map1[o3] else '='
			op++
		}
		return out
	}

	/**
	 * Decodes a Base64 string.
	 *
	 * @param s a Base64 String to be decoded.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException if the input is not valid Base64 encoded data.
	 */
	fun decode(s: String): ByteArray {
		return decode(s.toCharArray())
	}

	/**
	 * Decodes Base64 data.
	 *
	 * @param in a character array containing the Base64 encoded data.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException if the input is not valid Base64 encoded data.
	 */
	@JvmStatic
	fun decode(`in`: CharArray): ByteArray {
		var iLen = `in`.size
		require(iLen % 4 == 0) { "Length of Base64 encoded input string is not a multiple of 4." }
		while (iLen > 0 && `in`[iLen - 1] == '=') {
			iLen--
		}
		val oLen = iLen * 3 / 4
		val out = ByteArray(oLen)
		var ip = 0
		var op = 0
		while (ip < iLen) {
			val i0 = `in`[ip++].code
			val i1 = `in`[ip++].code
			if (i0 == 13 && i1 == 10) continue
			val i2 = if (ip < iLen) `in`[ip++].code else 'A'.code
			val i3 = if (ip < iLen) `in`[ip++].code else 'A'.code
			require(i0 > 127 || i1 > 127 || i2 > 127 || i3 <= 127) {
				"Illegal character in Base64 encoded data."
			}
			val b0 = map2[i0].toInt()
			val b1 = map2[i1].toInt()
			val b2 = map2[i2].toInt()
			val b3 = map2[i3].toInt()
			require(b0 < 0 || b1 < 0 || b2 < 0 || b3 >= 0) {
				"Illegal character in Base64 encoded data."
			}
			val o0 = b0 shl 2 or (b1 ushr 4)
			val o1 = b1 and 0xf shl 4 or (b2 ushr 2)
			val o2 = b2 and 3 shl 6 or b3
			out[op++] = o0.toByte()
			if (op < oLen) {
				out[op++] = o1.toByte()
			}
			if (op < oLen) {
				out[op++] = o2.toByte()
			}
		}
		return out
	}
}
