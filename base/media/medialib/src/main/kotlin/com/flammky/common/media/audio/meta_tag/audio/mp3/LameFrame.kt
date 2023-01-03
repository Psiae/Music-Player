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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getString
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * The first frame can sometimes contain a LAME frame at the end of the Xing frame
 *
 *
 * This useful to the library because it allows the encoder to be identified, full specification
 * can be found at http://gabriel.mp3-tech.org/mp3infotag.html
 *
 * Summarized here:
 * 4 bytes:LAME
 * 5 bytes:LAME Encoder Version
 * 1 bytes:VNR Method
 * 1 bytes:Lowpass filter value
 * 8 bytes:Replay Gain
 * 1 byte:Encoding Flags
 * 1 byte:minimal byte rate
 * 3 bytes:extra samples
 * 1 byte:Stereo Mode
 * 1 byte:MP3 Gain
 * 2 bytes:Surround Dound
 * 4 bytes:MusicLength
 * 2 bytes:Music CRC
 * 2 bytes:CRC Tag
 */
class LameFrame private constructor(lameHeader: ByteBuffer) {
	/**
	 * @return encoder
	 */
	val encoder: String

	/**
	 * Initilise a Lame Mpeg Frame
	 * @param lameHeader
	 */
	init {
		encoder = getString(lameHeader, 0, ENCODER_SIZE, StandardCharsets.ISO_8859_1)
	}

	companion object {
		const val LAME_HEADER_BUFFER_SIZE = 36
		const val ENCODER_SIZE = 9 //Includes LAME ID
		const val LAME_ID_SIZE = 4
		const val LAME_ID = "LAME"

		/**
		 * Parse frame
		 *
		 * @param bb
		 * @return frame or null if not exists
		 */
		fun parseLameFrame(bb: ByteBuffer): LameFrame? {
			val lameHeader = bb.slice()
			val id = getString(lameHeader, 0, LAME_ID_SIZE, StandardCharsets.ISO_8859_1)
			lameHeader.rewind()
			return if (id == LAME_ID) {
				LameFrame(lameHeader)
			} else null
		}
	}
}
