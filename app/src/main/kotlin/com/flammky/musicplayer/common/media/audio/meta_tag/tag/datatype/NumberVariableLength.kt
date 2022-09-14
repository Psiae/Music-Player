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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.getWholeNumber

/**
 * Represents a number which may span a number of bytes when written to file depending what size is to be represented.
 *
 * The bitorder in ID3v2 is most significant bit first (MSB). The byteorder in multibyte numbers is most significant
 * byte first (e.g. $12345678 would be encoded $12 34 56 78), also known as big endian and network byte order.
 *
 * In ID3Specification would be denoted as $xx xx xx xx (xx ...) , this denotes at least four bytes but may be more.
 * Sometimes may be completely optional (zero bytes)
 */
class NumberVariableLength : AbstractDataType {
	var minLength = MINIMUM_NO_OF_DIGITS

	/**
	 * Creates a new ObjectNumberVariableLength datatype, set minimum length to zero
	 * if this datatype is optional.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param minimumSize
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?, minimumSize: Int) : super(
		identifier,
		frameBody
	) {

		//Set minimum length, which can be zero if optional
		minLength = minimumSize
	}

	constructor(copy: NumberVariableLength) : super(copy) {
		minLength = copy.minLength
	}

	/**
	 * Return the maximum number of digits that can be used to express the number
	 *
	 * @return the maximum number of digits that can be used to express the number
	 */
	fun getMaximumLenth(): Int {
		return MAXIMUM_NO_OF_DIGITS
	}

	/**
	 * Return the  minimum  number of digits that can be used to express the number
	 *
	 * @return the minimum number of digits that can be used to express the number
	 */
	fun getMinimumLength(): Int {
		return minLength
	}

	/**
	 * @param minimumSize
	 */
	fun setMinimumSize(minimumSize: Int) {
		if (minimumSize > 0) {
			minLength = minimumSize
		}
	}

	override val size: Int
		get() {
			val value = this.value ?: return 0
			var current: Int
			var temp = getWholeNumber(value)
			var size = 0
			for (i in MINIMUM_NO_OF_DIGITS..MAXIMUM_NO_OF_DIGITS) {
				current = temp.toByte().toInt() and 0xFF
				if (current != 0) {
					size = i
				}
				temp = temp shr MAXIMUM_NO_OF_DIGITS
			}
			return if (minLength > size) minLength else size
		}


	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is NumberVariableLength) {
			return false
		}
		return minLength == other.minLength && super.equals(other)
	}

	/**
	 * Read from Byte Array
	 *
	 * @param arr
	 * @param offset
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		//Coding error, should never happen
		if (arr == null) {
			throw NullPointerException("Byte array is null")
		}

		//Coding error, should never happen as far as I can see
		require(offset >= 0) { "negativer offset into an array offset:$offset" }

		//If optional then set value to zero, this will mean that if this frame is written back to file it will be created
		//with this additional datatype wheras it didnt exist but I think this is probably an advantage the frame is
		//more likely to be parsed by other applications if it contains optional fields.
		//if not optional problem with this frame
		if (offset >= arr.size) {
			if (minLength == 0) {
				value = 0L
				return
			} else {
				throw InvalidDataTypeException("Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + arr.size)
			}
		}
		var lvalue: Long = 0

		//Read the bytes (starting from offset), the most significant byte of the number being constructed is read first,
		//we then shift the resulting long one byte over to make room for the next byte
		for (i in offset until arr.size) {
			lvalue = lvalue shl 8
			lvalue += (arr[i].toInt() and 0xff).toLong()
		}
		value = lvalue
	}

	/**
	 * @return String representation of the number
	 */
	override fun toString(): String {
		return if (value == null) {
			""
		} else {
			value.toString()
		}
	}

	/**
	 * Write to Byte Array
	 *
	 * @return the datatype converted to a byte array
	 */
	override fun writeByteArray(): ByteArray? {
		val size = this.size
		val arr: ByteArray
		if (size == 0) {
			arr = ByteArray(0)
		} else {
			var temp = getWholeNumber(
				value!!
			)
			arr = ByteArray(size)

			//keeps shifting the number downwards and masking the last 8 bist to get the value for the next byte
			//to be written
			for (i in size - 1 downTo 0) {
				arr[i] = (temp and 0xFFL).toByte()
				temp = temp shr 8
			}
		}
		return arr
	}

	companion object {
		private const val MINIMUM_NO_OF_DIGITS = 1
		private const val MAXIMUM_NO_OF_DIGITS = 8
	}
}
