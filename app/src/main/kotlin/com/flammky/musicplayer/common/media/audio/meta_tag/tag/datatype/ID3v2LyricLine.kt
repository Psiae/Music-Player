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
import java.nio.charset.StandardCharsets
import java.util.*

class ID3v2LyricLine : AbstractDataType {
	/**
	 *
	 */
	var text = ""

	/**
	 *
	 */
	var timeStamp: Long = 0

	override val size: Int
		get() = text.length + 1 + 4

	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	constructor(copy: ID3v2LyricLine) : super(copy) {
		text = copy.text
		timeStamp = copy.timeStamp
	}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is ID3v2LyricLine) {
			return false
		}
		val `object` = other
		return if (text != `object`.text) {
			false
		} else timeStamp == `object`.timeStamp && super.equals(other)
	}

	override fun hashCode(): Int = Objects.hash(text, timeStamp, super.hashCode())

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

		//offset += ();
		text = String(arr, offset, arr.size - offset - 4, StandardCharsets.ISO_8859_1)

		//text = text.substring(0, text.length() - 5);
		timeStamp = 0
		for (i in arr.size - 4 until arr.size) {
			timeStamp = timeStamp shl 8
			timeStamp += arr[i].toLong()
		}
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return "$timeStamp $text"
	}

	/**
	 * @return
	 */
	override fun writeByteArray(): ByteArray? {
		var i: Int
		val arr = ByteArray(size)
		i = 0
		while (i < text.length) {
			arr[i] = text[i].code.toByte()
			i++
		}
		arr[i++] = 0
		arr[i++] = (timeStamp and 0xFF000000L shr 24).toByte()
		arr[i++] = (timeStamp and 0x00FF0000L shr 16).toByte()
		arr[i++] = (timeStamp and 0x0000FF00L shr 8).toByte()
		arr[i++] = (timeStamp and 0x000000FFL).toByte()
		return arr
	}
}
