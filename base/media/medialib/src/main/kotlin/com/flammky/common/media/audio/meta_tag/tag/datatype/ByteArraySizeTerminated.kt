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
import java.util.logging.Level

/**
 * Represents a stream of bytes, continuing until the end of the buffer. Usually used for binary data or where
 * we havent yet mapped the data to a better fitting type.
 */
class ByteArraySizeTerminated : AbstractDataType {

	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(identifier, frameBody)
	constructor(`object`: ByteArraySizeTerminated?) : super(`object`)

	override val size: Int
		get() {
			var len = 0
			if (value != null) {
				len = (value as ByteArray).size
			}
			return len
		}


	override fun equals(other: Any?): Boolean {
		return other is ByteArraySizeTerminated && super.equals(other)
	}

	/**
	 * @param arr
	 * @param offset
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		if (arr == null) {
			throw NullPointerException("Byte array is null")
		}
		if (offset < 0) {
			throw IndexOutOfBoundsException("Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + arr.size)
		}

		//Empty Byte Array
		if (offset >= arr.size) {
			value = null
			return
		}
		val len = arr.size - offset
		value = ByteArray(len)
		System.arraycopy(arr, offset, value, 0, len)
	}

	/**
	 * Because this is usually binary data and could be very long we just return
	 * the number of bytes held
	 *
	 * @return the number of bytes
	 */
	override fun toString(): String {
		return "$size bytes"
	}

	/**
	 * Write contents to a byte array
	 *
	 * @return a byte array that that contians the data that should be perisisted to file
	 */
	override fun writeByteArray(): ByteArray? {
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config("Writing byte array$identifier")
		}
		return value as ByteArray
	}
}
