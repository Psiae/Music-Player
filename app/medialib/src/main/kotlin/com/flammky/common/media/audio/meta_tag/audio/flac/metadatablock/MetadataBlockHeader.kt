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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 * Metadata Block Header
 */
class MetadataBlockHeader {
	private var startByte //for debugging
		: Long = 0
	var isLastBlock: Boolean
		private set
	var dataLength = 0
		private set
	var bytes: ByteArray
		private set
	var blockType: BlockType? = null
		private set

	override fun toString(): String {
		return String.format(
			"StartByte:%d BlockType:%s DataLength:%d isLastBlock:%s",
			startByte,
			blockType,
			dataLength,
			isLastBlock
		)
	}

	/**
	 * Construct header by reading bytes
	 *
	 * @param rawdata
	 */
	constructor(startByte: Long, rawdata: ByteBuffer) {
		this.startByte = startByte
		isLastBlock = rawdata[0].toInt() and 0x80 ushr 7 == 1
		val type = rawdata[0].toInt() and 0x7F
		if (type < BlockType.values().size) {
			blockType = BlockType.values()[type]
			dataLength =
				(u(rawdata[1].toInt()) shl 16) + (u(rawdata[2].toInt()) shl 8) + u(rawdata[3].toInt())
			bytes = ByteArray(HEADER_LENGTH)
			for (i in 0 until HEADER_LENGTH) {
				bytes[i] = rawdata[i]
			}
		} else {
			throw CannotReadException(ErrorMessage.FLAC_NO_BLOCKTYPE.getMsg(type))
		}
	}

	/**
	 * Construct a new header in order to write metadatablock to file
	 *
	 * @param isLastBlock
	 * @param blockType
	 * @param dataLength
	 */
	constructor(isLastBlock: Boolean, blockType: BlockType, dataLength: Int) {
		val rawdata = ByteBuffer.allocate(HEADER_LENGTH)
		this.blockType = blockType
		this.isLastBlock = isLastBlock
		this.dataLength = dataLength
		val type: Byte
		type = if (isLastBlock) {
			(0x80 or blockType.id).toByte()
		} else {
			blockType.id.toByte()
		}
		rawdata.put(type)

		//Size is 3Byte BigEndian int
		rawdata.put((dataLength and 0xFF0000 ushr 16).toByte())
		rawdata.put((dataLength and 0xFF00 ushr 8).toByte())
		rawdata.put((dataLength and 0xFF).toByte())
		bytes = ByteArray(HEADER_LENGTH)
		for (i in 0 until HEADER_LENGTH) {
			bytes[i] = rawdata[i]
		}
	}

	private fun u(i: Int): Int {
		return i and 0xFF
	}

	val bytesWithoutIsLastBlockFlag: ByteArray
		get() {
			bytes[0] = (bytes[0].toInt() and 0x7F).toByte()
			return bytes
		}
	val bytesWithLastBlockFlag: ByteArray
		get() {
			bytes[0] = (bytes[0].toInt() or 0x80).toByte()
			return bytes
		}

	companion object {
		const val BLOCK_TYPE_LENGTH = 1
		const val BLOCK_LENGTH = 3
		const val HEADER_LENGTH = BLOCK_TYPE_LENGTH + BLOCK_LENGTH
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")

		/**
		 * Create header by reading from file
		 *
		 * @param fc
		 * @return
		 * @throws IOException
		 */
		@JvmStatic
		@Throws(CannotReadException::class, IOException::class)
		fun readHeader(fc: FileChannel): MetadataBlockHeader {
			val rawdata = ByteBuffer.allocate(HEADER_LENGTH)
			val startByte = fc.position()
			val bytesRead = fc.read(rawdata)
			if (bytesRead < HEADER_LENGTH) {
				throw IOException("Unable to read required number of databytes read:" + bytesRead + ":required:" + HEADER_LENGTH)
			}
			rawdata.rewind()
			return MetadataBlockHeader(startByte, rawdata)
		}
	}
}
