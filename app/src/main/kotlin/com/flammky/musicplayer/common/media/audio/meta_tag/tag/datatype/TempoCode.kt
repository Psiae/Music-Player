/**
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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.getWholeNumber

/**
 * Represents a [FrameBodySYTC] tempo code.
 *
 * The tempo is in BPM described with one or two bytes. If the
 * first byte has the value $FF, one more byte follows, which is added
 * to the first giving a range from 2 - 510 BPM, since $00 and $01 is
 * reserved. $00 is used to describe a beat-free time period, which is
 * not the same as a music-free time period. $01 is used to indicate one
 * single beat-stroke followed by a beat-free period.
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 * @version $Id:$
 */
class TempoCode : AbstractDataType {
	constructor(copy: TempoCode?) : super(copy)
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody,
		0
	)

	constructor(identifier: String?, frameBody: AbstractTagFrameBody?, value: Any?) : super(
		identifier,
		frameBody,
		value
	)

	override val size: Int
		get() = value
			?.let { if (getWholeNumber(it) < 0xFF) MINIMUM_NO_OF_DIGITS else MAXIMUM_NO_OF_DIGITS }
			?: 0


	override fun equals(that: Any?): Boolean {
		return that is TempoCode && super.equals(that)
	}

	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		if (arr == null) {
			throw NullPointerException("Byte array is null")
		}
		require(offset >= 0) { "negative offset into an array offset:$offset" }
		if (offset >= arr.size) {
			throw InvalidDataTypeException("Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + arr.size)
		}
		var lvalue: Long = 0
		lvalue += (arr[offset].toInt() and 0xff).toLong()
		if (lvalue == 0xFFL) {
			lvalue += (arr[offset + 1].toInt() and 0xff).toLong()
		}
		value = lvalue
	}

	override fun writeByteArray(): ByteArray {
		val size = this.size
		val arr = ByteArray(size)
		var temp = getWholeNumber(
			value!!
		)
		var offset = 0
		if (temp >= 0xFF) {
			arr[offset] = 0xFF.toByte()
			offset++
			temp -= 0xFF
		}
		arr[offset] = (temp and 0xFFL).toByte()
		return arr
	}

	override fun toString(): String {
		return if (value == null) "" else value.toString()
	}

	companion object {
		private const val MINIMUM_NO_OF_DIGITS = 1
		private const val MAXIMUM_NO_OF_DIGITS = 2
	}
}
