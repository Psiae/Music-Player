/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.ogg.util

import java.util.logging.Logger

/**
 * OffCRC Calculations
 *
 * $Id$
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @version 19 d�cembre 2003
 */
class OggCRCFactory {
	fun checkCRC(data: ByteArray, crc: ByteArray?): Boolean {
		return String(crc!!) == String(computeCRC(data))
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.ogg")
		private val crc_lookup = LongArray(256)
		private var init = false
		fun init() {
			for (i in 0..255) {
				var r = (i shl 24).toLong()
				for (j in 0..7) {
					r = if (r and 0x80000000L != 0L) {
						r shl 1 xor 0x04c11db7L
					} else {
						r shl 1
					}
				}
				crc_lookup[i] = r
			}
			init = true
		}

		@JvmStatic
		fun computeCRC(data: ByteArray): ByteArray {
			if (!init) {
				init()
			}
			var crc_reg: Long = 0
			for (aData in data) {
				val tmp = (crc_reg ushr 24 and 0xffL xor u(aData.toInt()).toLong()).toInt()
				crc_reg = crc_reg shl 8 xor crc_lookup[tmp]
				crc_reg = crc_reg and 0xffffffffL
			}
			val sum = ByteArray(4)
			sum[0] = (crc_reg and 0xffL).toByte()
			sum[1] = (crc_reg ushr 8 and 0xffL).toByte()
			sum[2] = (crc_reg ushr 16 and 0xffL).toByte()
			sum[3] = (crc_reg ushr 24 and 0xffL).toByte()
			return sum
		}

		private fun u(n: Int): Int {
			return n and 0xff
		}
	}
}
