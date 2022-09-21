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
 * Represents a Number of a fixed number of decimal places.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.getWholeNumber
import java.util.logging.Level

/**
 * Represents a number held as a fixed number of digits.
 *
 * The bitorder in ID3v2 is most significant bit first (MSB). The byteorder in multibyte numbers is most significant
 * byte first (e.g. $12345678 would be encoded $12 34 56 78), also known as big endian and network byte order.
 *
 * In ID3Specification would be denoted as $xx xx this denotes exactly two bytes required
 */
open class NumberFixedLength : AbstractDataType {

	override var size: Int = super.size
		set(value) {
			if (value < 0) return
			field = value
		}

	/**
	 * Creates a new ObjectNumberFixedLength datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param size       the number of significant places that the number is held to
	 * @throws IllegalArgumentException
	 */
	constructor(
		identifier: String?,
		frameBody: AbstractTagFrameBody?,
		size: Int
	) : super(identifier, frameBody) {
		require(size >= 0) { "Length is less than zero: $size" }
		this.size = size
	}

	constructor(copy: NumberFixedLength) : super(copy) {
		size = copy.size
	}

	override var value: Any? = super.value
		set(value) {
			require(value is Number) { "Invalid value type for NumberFixedLength:" + value!!.javaClass }
			field = value
		}


	/**
	 * @param other
	 * @return true if obj equivalent to this
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is NumberFixedLength) {
			return false
		}
		return size == other.size && super.equals(other)
	}

	/**
	 * Read the number from the byte array
	 *
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
		if (offset < 0 || offset >= arr.size) {
			throw InvalidDataTypeException("Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + arr.size)
		}
		if (offset + size > arr.size) {
			throw InvalidDataTypeException(
				"Offset plus size to byte array is out of bounds: offset = "
					+ offset + ", size = " + size + " + arr.length " + arr.size
			)
		}
		var lvalue: Long = 0
		for (i in offset until offset + size) {
			lvalue = lvalue shl 8
			lvalue += (arr[i].toInt() and 0xff).toLong()
		}
		value = lvalue
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config(
				"Read NumberFixedlength:$value"
			)
		}
	}

	/**
	 * @return String representation of this datatype
	 */
	override fun toString(): String {
		return if (value == null) {
			""
		} else {
			value.toString()
		}
	}

	/**
	 * Write data to byte array
	 *
	 * @return the datatype converted to a byte array
	 */
	override fun writeByteArray(): ByteArray? {
		val arr: ByteArray
		arr = ByteArray(size)
		if (value != null) {
			//Convert value to long
			var temp = getWholeNumber(
				value!!
			)
			for (i in size - 1 downTo 0) {
				arr[i] = (temp and 0xFFL).toByte()
				temp = temp shr 8
			}
		}
		return arr
	}
}
