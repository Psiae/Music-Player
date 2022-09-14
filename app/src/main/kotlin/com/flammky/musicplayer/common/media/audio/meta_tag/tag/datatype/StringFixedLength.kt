/**
 * @author : Paul Taylor
 * @author : Eric Farng
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets

/**
 * Represents a fixed length String, whereby the length of the String is known. The String
 * will be encoded based upon the text encoding of the frame that it belongs to.
 */
open class StringFixedLength : AbstractString {
	/**
	 * Creates a new ObjectStringFixedsize datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param size
	 * @throws IllegalArgumentException
	 */
	constructor(
		identifier: String?,
		frameBody: AbstractTagFrameBody?,
		size: Int
	) : super(identifier, frameBody) {
		require(size >= 0) { "size is less than zero: $size" }
		this.size = size
	}

	constructor(copyObject: StringFixedLength) : super(copyObject) {
		size = copyObject.size
	}

	/**
	 * @param other
	 * @return if obj is equivalent to this
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is StringFixedLength) {
			return false
		}
		return size == other.size && super.equals(other)
	}

	/**
	 * Read a string from buffer of fixed size(size has already been set in constructor)
	 *
	 * @param arr    this is the buffer for the frame
	 * @param offset this is where to start reading in the buffer for this field
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		logger.config(
			"Reading from array from offset:$offset"
		)
		value = try {
			val decoder = textEncodingCharSet!!.newDecoder()

			//Decode buffer if runs into problems should through exception which we
			//catch and then set value to empty string.
			logger.finest("Array length is:" + arr!!.size + "offset is:" + offset + "Size is:" + size)
			if (arr.size - offset < size) {
				throw InvalidDataTypeException(
					"byte array is to small to retrieve string of declared length:$size"
				)
			}
			val str = decoder.decode(ByteBuffer.wrap(arr, offset, size)).toString()
			str
		} catch (ce: CharacterCodingException) {
			logger.severe(ce.message)
			""
		}
		logger.config(
			"Read StringFixedLength:$value"
		)
	}

	/**
	 * Write String into byte array
	 *
	 * The string will be adjusted to ensure the correct number of bytes are written, If the current value is null
	 * or to short the written value will have the 'space' character appended to ensure this. We write this instead of
	 * the null character because the null character is likely to confuse the parser into misreading the next field.
	 *
	 * @return the byte array to be written to the file
	 */
	override fun writeByteArray(): ByteArray? {
		val dataBuffer: ByteBuffer?
		val data: ByteArray

		//Create with a series of empty of spaces to try and ensure integrity of field
		if (value == null) {
			logger.warning("Value of StringFixedlength Field is null using default value instead")
			data = ByteArray(size)
			for (i in 0 until size) {
				data[i] = ' '.code.toByte()
			}
			return data
		}
		try {
			val charset = textEncodingCharSet
			val encoder: CharsetEncoder
			if (StandardCharsets.UTF_16 == charset) {
				//Note remember LE BOM is ff fe but tis is handled by encoder Unicode char is fe ff
				encoder = StandardCharsets.UTF_16LE.newEncoder()
				dataBuffer = encoder.encode(CharBuffer.wrap('\ufeff'.toString() + value as String))
			} else {
				encoder = charset!!.newEncoder()
				dataBuffer = encoder.encode(CharBuffer.wrap(value as String))
			}
		} catch (ce: CharacterCodingException) {
			logger.warning("There was a problem writing the following StringFixedlength Field:" + value + ":" + ce.message + "using default value instead")
			data = ByteArray(size)
			var i = 0
			while (i < size) {
				data[i] = ' '.code.toByte()
				i++
			}
			return data
		}

		// We must return the defined size.
		// To check now because size is in bytes not chars
		return if (dataBuffer != null) {
			//Everything ok
			if (dataBuffer.limit() == size) {
				data = ByteArray(dataBuffer.limit())
				dataBuffer[data, 0, dataBuffer.limit()]
				data
			} else if (dataBuffer.limit() > size) {
				logger.warning("There was a problem writing the following StringFixedlength Field:" + value + " when converted to bytes has length of:" + dataBuffer.limit() + " but field was defined with length of:" + size + " too long so stripping extra length")
				data = ByteArray(size)
				dataBuffer[data, 0, size]
				data
			} else {
				logger.warning("There was a problem writing the following StringFixedlength Field:" + value + " when converted to bytes has length of:" + dataBuffer.limit() + " but field was defined with length of:" + size + " too short so padding with spaces to make up extra length")
				data = ByteArray(size)
				dataBuffer[data, 0, dataBuffer.limit()]
				for (i in dataBuffer.limit() until size) {
					data[i] = ' '.code.toByte()
				}
				data
			}
		} else {
			logger.warning(
				"There was a serious problem writing the following StringFixedlength Field:$value:using default value instead"
			)
			data = ByteArray(size)
			for (i in 0 until size) {
				data[i] = ' '.code.toByte()
			}
			data
		}
	}

	/**
	 * @return the encoding of the frame body this datatype belongs to
	 */
	override val textEncodingCharSet: Charset?
		protected get() {
			val textEncoding = this.body?.textEncoding ?: return null
			val charset = TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
			logger.finest("text encoding:" + textEncoding + " charset:" + charset!!.name())
			return charset
		}
}
