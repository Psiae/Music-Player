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

/**
 * Represents a bit flag within a byte
 */
class BooleanByte : AbstractDataType {
	/**
	 *
	 */
	var bitPosition = -1
		private set

	/**
	 * Creates a new ObjectBooleanByte datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param bitPosition
	 * @throws IndexOutOfBoundsException
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?, bitPosition: Int) : super(
		identifier,
		frameBody
	) {
		if (bitPosition < 0 || bitPosition > 7) {
			throw IndexOutOfBoundsException("Bit position needs to be from 0 - 7 : $bitPosition")
		}
		this.bitPosition = bitPosition
	}

	constructor(copy: BooleanByte) : super(copy) {
		bitPosition = copy.bitPosition
	}

	override val size: Int
		get() = 1

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is BooleanByte) {
			return false
		}
		return bitPosition == other.bitPosition && super.equals(other)
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
		if (offset < 0 || offset >= arr.size) {
			throw IndexOutOfBoundsException("Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + arr.size)
		}
		var newValue = arr[offset]
		newValue = (newValue.toInt() shr bitPosition).toByte()
		newValue = (newValue.toInt() and 0x1).toByte()
		value = newValue.toInt() == 1
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return "" + value
	}

	/**
	 * @return
	 */
	override fun writeByteArray(): ByteArray? {
		val retValue: ByteArray = ByteArray(1)
		if (value != null) {
			retValue[0] = (if (value as Boolean) 1 else 0).toByte()
			retValue[0] = (retValue[0].toInt() shl bitPosition).toByte()
		}
		return retValue
	}
}
