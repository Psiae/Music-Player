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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidBoxHeaderException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NullBoxIdException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readFourBytesAsChars
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Everything in MP4s are held in boxes (formally known as atoms), they are held as a hierachial tree within the MP4.
 *
 * We are most interested in boxes that are used to hold metadata, but we have to know about some other boxes
 * as well in order to find them.
 *
 * All boxes consist of a 4 byte box length (big Endian), and then a 4 byte identifier, this is the header
 * which is model in this class.
 *
 * The length includes the length of the box including the header itself.
 * Then they may contain data and/or sub boxes, if they contain subboxes they are known as a parent box. Parent boxes
 * shouldn't really contain data, but sometimes they do.
 *
 * Parent boxes length includes the length of their immediate sub boxes
 *
 * This class is normally used by instantiating with the empty constructor, then use the update method
 * to pass the header data which is used to read the identifier and the the size of the box
 */
open class Mp4BoxHeader {
	/**
	 * @return the box identifier
	 */
	//Box identifier
	var id: String? = null
		private set

	//Box length
	private var _length = 0

	var length: Int
		get() = _length
		set(value) {
			/**
			 * Set the length.
			 *
			 * This will modify the dataBuffer accordingly
			 */
			val headerSize = getSizeBEInt32(value)
			val dataBuffer = dataBuffer!!
			dataBuffer.put(0, headerSize[0])
			dataBuffer.put(1, headerSize[1])
			dataBuffer.put(2, headerSize[2])
			dataBuffer.put(3, headerSize[3])
			_length = value
		}


	/**
	 * @return location in file of the start of atom  header (i.e where the 4 byte length field starts)
	 */
	/**
	 * Set location in file of the start of file header (i.e where the 4 byte length field starts)
	 *
	 * @param filePos
	 */
	//If reading from file , this can be used to hold the headers position in the file
	var filePos: Long = 0

	//Raw Header data
	protected var dataBuffer: ByteBuffer? = null

	/**
	 * Construct empty header
	 *
	 * Can be populated later with update method
	 */
	constructor()

	/**
	 * Construct header to allow manual creation of header for writing to file
	 *
	 * @param id
	 */
	constructor(id: String) {
		if (id.length != IDENTIFIER_LENGTH) {
			throw RuntimeException("Invalid length:atom idenifier should always be 4 characters long")
		}
		dataBuffer = ByteBuffer.allocate(HEADER_LENGTH)
		val dataBuffer = dataBuffer!!
		try {
			this.id = id
			dataBuffer.put(4, id.toByteArray(charset("ISO-8859-1"))[0])
			dataBuffer.put(5, id.toByteArray(charset("ISO-8859-1"))[1])
			dataBuffer.put(6, id.toByteArray(charset("ISO-8859-1"))[2])
			dataBuffer.put(7, id.toByteArray(charset("ISO-8859-1"))[3])
		} catch (uee: UnsupportedEncodingException) {
			//Should never happen
			throw RuntimeException(uee)
		}
	}

	/**
	 * Construct header
	 *
	 * Create header using headerdata, expected to find header at headerdata current position
	 *
	 * Note after processing adjusts position to immediately after header
	 *
	 * @param headerData
	 */
	constructor(headerData: ByteBuffer) {
		update(headerData)
	}

	/**
	 * Create header using headerdata, expected to find header at headerdata current position
	 *
	 * Note after processing adjusts position to immediately after header
	 *
	 * @param headerData
	 */
	fun update(headerData: ByteBuffer) {
		//Read header data into byte array
		val b = ByteArray(HEADER_LENGTH)
		headerData[b]
		//Keep reference to copy of RawData
		dataBuffer = ByteBuffer.wrap(b)
		val dataBuffer = dataBuffer!!

		dataBuffer.order(ByteOrder.BIG_ENDIAN)

		//Calculate box size and id
		_length = dataBuffer.int
		id = readFourBytesAsChars(dataBuffer)
		println("Mp4BoxHeader id:$id length:$length ${b.size}")
		if (id == "\u0000\u0000\u0000\u0000") {
			throw NullBoxIdException(
				ErrorMessage.MP4_UNABLE_TO_FIND_NEXT_ATOM_BECAUSE_IDENTIFIER_IS_INVALID.getMsg(
					id
				)
			)
		}
		if (length < HEADER_LENGTH) {
			if (length == 1) {
				//Indicates 64bit, we need to read body to find true length
			} else {
				throw InvalidBoxHeaderException(
					ErrorMessage.MP4_UNABLE_TO_FIND_NEXT_ATOM_BECAUSE_IDENTIFIER_IS_INVALID.getMsg(
						id,
						length
					)
				)
			}
		}
	}

	/**
	 * Set the Id.
	 *
	 * Allows you to manully create a header
	 * This will modify the databuffer accordingly
	 *
	 * @param length
	 */
	fun setId(length: Int) {
		val headerSize = getSizeBEInt32(length)
		dataBuffer!!.put(5, headerSize[0])
		dataBuffer!!.put(6, headerSize[1])
		dataBuffer!!.put(7, headerSize[2])
		dataBuffer!!.put(8, headerSize[3])
		_length = length
	}

	/**
	 * @return the 8 byte header buffer
	 */
	val headerData: ByteBuffer?
		get() {
			dataBuffer!!.rewind()
			return dataBuffer
		}

	/**
	 * @return the length of the data only (does not include the header size)
	 */
	val dataLength: Int
		get() = length - HEADER_LENGTH

	override fun toString(): String {
		return "Box $id:length$length:filepos:$filePos"
	}

	/**
	 * @return UTF_8 (always used by Mp4)
	 */
	val encoding: Charset
		get() = StandardCharsets.UTF_8

	/**
	 *
	 * @return location in file of the end of atom
	 */
	val fileEndPos: Long
		get() = filePos + length

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.mp4.atom")
		const val OFFSET_POS = 0
		const val IDENTIFIER_POS = 4
		const val OFFSET_LENGTH = 4
		const val IDENTIFIER_LENGTH = 4
		const val HEADER_LENGTH = OFFSET_LENGTH + IDENTIFIER_LENGTH
		const val DATA_64BITLENGTH = 8
		const val REALDATA_64BITLENGTH = HEADER_LENGTH + DATA_64BITLENGTH

		//Mp4 uses UTF-8 for all text
		const val CHARSET_UTF_8 = "UTF-8"

		/**
		 * Seek for box with the specified id starting from the current location of filepointer,
		 *
		 * Note it wont find the box if it is contained with a level below the current level, nor if we are
		 * at a parent atom that also contains data and we havent yet processed the data. It will work
		 * if we are at the start of a child box even if it not the required box as long as the box we are
		 * looking for is the same level (or the level above in some cases).
		 *
		 * @param fc
		 * @param id
		 * @throws IOException
		 * @return
		 */
		@Throws(IOException::class)
		fun seekWithinLevel(fc: SeekableByteChannel, id: String): Mp4BoxHeader? {
			println("Started searching for:" + id + " in file at:" + fc.position())
			val boxHeader = Mp4BoxHeader()
			val headerBuffer = ByteBuffer.allocate(HEADER_LENGTH)
			var bytesRead = fc.read(headerBuffer)
			if (bytesRead != HEADER_LENGTH) {
				return null
			}
			headerBuffer.rewind()
			boxHeader.update(headerBuffer)
			while (boxHeader.id != id) {
				logger.finer("Found:" + boxHeader.id + " Still searching for:" + id + " in file at:" + fc.position())

				//64bit data length
				if (boxHeader.length == 1) {
					val data64bitLengthBuffer = ByteBuffer.allocate(DATA_64BITLENGTH)
					data64bitLengthBuffer.order(ByteOrder.BIG_ENDIAN)
					bytesRead = fc.read(data64bitLengthBuffer)
					if (bytesRead != DATA_64BITLENGTH) {
						return null
					}
					data64bitLengthBuffer.rewind()
					val length = data64bitLengthBuffer.long
					if (length < HEADER_LENGTH) {
						return null
					}
					fc.position(fc.position() + length - REALDATA_64BITLENGTH)
					logger.severe("Skipped 64bit data length, now at:" + fc.position())
				} else if (boxHeader.length < HEADER_LENGTH) {
					return null
				} else {
					fc.position(fc.position() + boxHeader.dataLength)
				}
				if (fc.position() > fc.size()) {
					return null
				}
				headerBuffer.rewind()
				bytesRead = fc.read(headerBuffer)
				logger.finer(
					"Header Bytes Read:$bytesRead"
				)
				headerBuffer.rewind()
				if (bytesRead == HEADER_LENGTH) {
					boxHeader.update(headerBuffer)
				} else {
					return null
				}
			}
			return boxHeader
		}

		/**
		 * Seek for box with the specified id starting from the current location of filepointer,
		 *
		 * Note it won't find the box if it is contained with a level below the current level, nor if we are
		 * at a parent atom that also contains data and we havent yet processed the data. It will work
		 * if we are at the start of a child box even if it not the required box as long as the box we are
		 * looking for is the same level (or the level above in some cases).
		 *
		 * @param data
		 * @param id
		 * @throws IOException
		 * @return
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun seekWithinLevel(data: ByteBuffer, id: String): Mp4BoxHeader? {
			logger.finer("Started searching for:" + id + " in bytebuffer at" + data.position())
			val boxHeader = Mp4BoxHeader()
			if (data.remaining() >= HEADER_LENGTH) {
				boxHeader.update(data)
			} else {
				return null
			}
			while (boxHeader.id != id) {
				logger.finer("Found:" + boxHeader.id + " Still searching for:" + id + " in bytebuffer at" + data.position())
				//Something gone wrong probably not at the start of an atom so return null;
				if (boxHeader.length < HEADER_LENGTH) {
					return null
				}
				if (data.remaining() < boxHeader.length - HEADER_LENGTH) {
					//i.e Could happen if Moov header had size incorrectly recorded
					return null
				}
				data.position(data.position() + (boxHeader.length - HEADER_LENGTH))
				if (data.remaining() >= HEADER_LENGTH) {
					boxHeader.update(data)
				} else {
					return null
				}
			}
			logger.finer("Found:" + id + " in bytebuffer at" + data.position())
			return boxHeader
		}
	}
}
