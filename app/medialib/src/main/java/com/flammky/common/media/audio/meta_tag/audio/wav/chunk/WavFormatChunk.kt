/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.u
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavSubFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.Hex
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Logger

/**
 * Reads the fmt header, this contains the information required for constructing Audio header
 *
 * 0 - 1   ushort SubFormatIdentifier;
 * 2 - 3   ushort NoOfChannels;
 * 4 - 7   uint   NoOfSamplesPerSec;
 * 8 - 11  uint   AverageNoBytesPerSec;
 * 12 - 13 ushort BlockAlign;
 * 14 - 15 ushort NoofBitsPerSample;
 * //May be additional fields here, depending upon wFormatTag.
 * } FormatChunk
 */
class WavFormatChunk(
	chunkData: ByteBuffer?,
	hdr: ChunkHeader?,
	private val info: GenericAudioHeader
) : Chunk(
	chunkData!!, hdr!!
) {
	private val isValid = false
	private var blockAlign = 0
	private var channelMask = 0
	private var wsf: WavSubFormat? = null

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		val subFormatCode = u(chunkData.short)
		wsf = WavSubFormat.getByCode(subFormatCode)
		info.channelNumber = u(chunkData.short)
		info.setSamplingRate(chunkData.int)
		info.byteRate = chunkData.int
		info.setBitRate(info.byteRate!! * Utils.BITS_IN_BYTE_MULTIPLIER / Utils.KILOBYTE_MULTIPLIER) //AvgBytePerSec  converted to kb/sec
		info.isVariableBitRate = false
		blockAlign = u(chunkData.short)
		info.bitsPerSample =
			u(chunkData.short)
		if (wsf != null && wsf == WavSubFormat.FORMAT_EXTENSIBLE) {
			val extensibleSize = u(chunkData.short)
			if (extensibleSize == EXTENSIBLE_DATA_SIZE) {
				info.bitsPerSample =
					u(chunkData.short)
				//We dont use this currently
				channelMask = chunkData.int

				//If Extensible then the actual formatCode is held here
				wsf = WavSubFormat.getByCode(u(chunkData.short))
			}
		}
		if (wsf != null) {
			if (info.bitsPerSample > 0) {
				info.encodingType = (wsf!!.description + " " + info.bitsPerSample + " bits")
			} else {
				info.encodingType = (wsf!!.description)
			}
		} else {
			info.encodingType = ("Unknown Sub Format Code:" + Hex.asHex(subFormatCode))
		}
		//logger.severe(info.toString());
		return true
	}

	override fun toString(): String {
		var out = "RIFF-WAVE Header:\n"
		out += "Is valid?: $isValid"
		return out
	}

	companion object {
		private const val EXTENSIBLE_DATA_SIZE = 22
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav.chunk")
	}
}
