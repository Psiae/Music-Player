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

class Lyrics3TimeStamp : AbstractDataType {
	/**
	 *
	 */
	private var minute: Long = 0

	/**
	 *
	 */
	private var second: Long = 0

	/**
	 * Todo this is wrong
	 * @param s
	 */
	fun readString(s: String?) {}

	/**
	 * Creates a new ObjectLyrics3TimeStamp datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	constructor(identifier: String?) : super(identifier, null)
	constructor(copy: Lyrics3TimeStamp?) : super(copy) {
		minute = copy!!.minute
		second = copy.second
	}

	fun setMinute(minute: Long) {
		this.minute = minute
	}

	/**
	 * @return
	 */
	fun getMinute(): Long {
		return minute
	}

	fun setSecond(second: Long) {
		this.second = second
	}

	/**
	 * @return
	 */
	fun getSecond(): Long {
		return second
	}

	override val size: Int
		get() = 7

	/**
	 * Creates a new ObjectLyrics3TimeStamp datatype.
	 *
	 * @param timeStamp
	 * @param timeStampFormat
	 */
	fun setTimeStamp(timeStamp: Long, timeStampFormat: Byte) {
		/**
		 * @todo convert both types of formats
		 */
		var timeStamp = timeStamp
		timeStamp = timeStamp / 1000
		minute = timeStamp / 60
		second = timeStamp % 60
	}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is Lyrics3TimeStamp) {
			return false
		}
		val `object` = other
		return if (minute != `object`.minute) {
			false
		} else second == `object`.second && super.equals(other)
	}

	/**
	 * @param timeStamp
	 * @param offset
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	fun readString(timeStamp: String?, offset: Int) {
		var timeStamp = timeStamp ?: throw NullPointerException("Image is null")
		if (offset < 0 || offset >= timeStamp.length) {
			throw IndexOutOfBoundsException("Offset to timeStamp is out of bounds: offset = " + offset + ", timeStamp.length()" + timeStamp.length)
		}
		timeStamp = timeStamp.substring(offset)
		if (timeStamp.length == 7) {
			minute = timeStamp.substring(1, 3).toInt().toLong()
			second = timeStamp.substring(4, 6).toInt().toLong()
		} else {
			minute = 0
			second = 0
		}
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return writeString()
	}

	/**
	 * @return
	 */
	fun writeString(): String {
		var str: String?
		str = "["
		if (minute < 0) {
			str += "00"
		} else {
			if (minute < 10) {
				str += '0'
			}
			str += java.lang.Long.toString(minute)
		}
		str += ':'
		if (second < 0) {
			str += "00"
		} else {
			if (second < 10) {
				str += '0'
			}
			str += java.lang.Long.toString(second)
		}
		str += ']'
		return str
	}

	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		readString(arr.toString(), offset)
	}

	override fun writeByteArray(): ByteArray? {
		return writeString().toByteArray(StandardCharsets.ISO_8859_1)
	}
}
