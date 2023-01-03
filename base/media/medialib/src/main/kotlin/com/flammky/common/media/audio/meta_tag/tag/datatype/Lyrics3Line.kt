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

class Lyrics3Line : AbstractDataType {
	/**
	 *
	 */
	private var timeStamp: MutableList<Lyrics3TimeStamp> = LinkedList()

	/**
	 *
	 */
	private var lyric: String? = ""

	/**
	 * Creates a new ObjectLyrics3Line datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	constructor(copy: Lyrics3Line) : super(copy) {
		lyric = copy.lyric
		var newTimeStamp: Lyrics3TimeStamp
		for (i in copy.timeStamp.indices) {
			newTimeStamp = Lyrics3TimeStamp(
				copy.timeStamp[i]
			)
			timeStamp.add(newTimeStamp)
		}
	}

	fun setLyric(lyric: String?) {
		this.lyric = lyric
	}

	fun setLyric(line: ID3v2LyricLine) {
		lyric = line.text
	}

	/**
	 * @return
	 */
	fun getLyric(): String? {
		return lyric
	}

	override val size: Int
		get() {
			var size = 0
			for (aTimeStamp in timeStamp) {
				size += aTimeStamp.size
			}
			return size + (lyric?.length ?: 0)
		}

	/**
	 * @param time
	 */
	fun setTimeStamp(time: Lyrics3TimeStamp) {
		timeStamp.clear()
		timeStamp.add(time)
	}

	/**
	 * @return
	 */
	fun getTimeStamp(): Iterator<Lyrics3TimeStamp> {
		return timeStamp.iterator()
	}

	fun addLyric(newLyric: String?) {
		lyric += newLyric
	}

	fun addLyric(line: ID3v2LyricLine) {
		lyric += line.text
	}

	/**
	 * @param time
	 */
	fun addTimeStamp(time: Lyrics3TimeStamp) {
		timeStamp.add(time)
	}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is Lyrics3Line) {
			return false
		}
		val `object` = other
		return if (lyric != `object`.lyric) {
			false
		} else timeStamp == `object`.timeStamp && super.equals(other)
	}

	/**
	 * @return
	 */
	fun hasTimeStamp(): Boolean {
		return !timeStamp.isEmpty()
	}

	/**
	 * @param lineString
	 * @param offset
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	fun readString(lineString: String?, offset: Int) {
		var offset = offset
		if (lineString == null) {
			throw NullPointerException("Image is null")
		}
		if (offset < 0 || offset >= lineString.length) {
			throw IndexOutOfBoundsException("Offset to line is out of bounds: offset = " + offset + ", line.length()" + lineString.length)
		}
		var delim: Int
		var time: Lyrics3TimeStamp
		timeStamp = LinkedList()
		delim = lineString.indexOf("[", offset)
		while (delim >= 0) {
			offset = lineString.indexOf("]", delim) + 1
			time = Lyrics3TimeStamp("Time Stamp")
			time.readString(lineString.substring(delim, offset))
			timeStamp.add(time)
			delim = lineString.indexOf("[", offset)
		}
		lyric = lineString.substring(offset)
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		var str = ""
		for (aTimeStamp in timeStamp) {
			str += aTimeStamp.toString()
		}
		return "timeStamp = $str, lyric = $lyric\n"
	}

	/**
	 * @return
	 */
	fun writeString(): String {
		var str = ""
		var time: Lyrics3TimeStamp
		for (aTimeStamp in timeStamp) {
			time = aTimeStamp
			str += time.writeString()
		}
		return str + lyric
	}

	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		readString(arr.toString(), offset)
	}

	override fun writeByteArray(): ByteArray? {
		return writeString().toByteArray(StandardCharsets.ISO_8859_1)
	}
}
