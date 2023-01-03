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

class Lyrics3Image : AbstractDataType {
	/**
	 *
	 */
	private var time: Lyrics3TimeStamp? = null

	/**
	 *
	 */
	private var description: String? = ""

	/**
	 *
	 */
	private var filename: String? = ""

	/**
	 * Creates a new ObjectLyrics3Image datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 */
	constructor(identifier: String?, frameBody: AbstractTagFrameBody?) : super(
		identifier,
		frameBody
	)

	constructor(copy: Lyrics3Image) : super(copy) {
		time = Lyrics3TimeStamp(copy.time)
		description = copy.description
		filename = copy.filename
	}

	/**
	 * @param description
	 */
	fun setDescription(description: String?) {
		this.description = description
	}

	/**
	 * @return
	 */
	fun getDescription(): String? {
		return description
	}

	/**
	 * @param filename
	 */
	fun setFilename(filename: String?) {
		this.filename = filename
	}

	/**
	 * @return
	 */
	fun getFilename(): String? {
		return filename
	}

	override val size: Int
		get() {
			var size: Int = filename!!.length + 2 + description!!.length + 2
			if (time != null) {
				size += time!!.size
			}
			return size
		}

	/**
	 * @param time
	 */
	fun setTimeStamp(time: Lyrics3TimeStamp?) {
		this.time = time
	}

	/**
	 * @return
	 */
	fun getTimeStamp(): Lyrics3TimeStamp? {
		return time
	}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is Lyrics3Image) {
			return false
		}
		val `object` = other
		if (description != `object`.description) {
			return false
		}
		if (filename != `object`.filename) {
			return false
		}
		if (time == null) {
			if (`object`.time != null) {
				return false
			}
		} else {
			if (time != `object`.time) {
				return false
			}
		}
		return super.equals(other)
	}

	/**
	 * @param imageString
	 * @param offset
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	fun readString(imageString: String?, offset: Int) {
		var offset = offset
		if (imageString == null) {
			throw NullPointerException("Image string is null")
		}
		if (offset < 0 || offset >= imageString.length) {
			throw IndexOutOfBoundsException("Offset to image string is out of bounds: offset = " + offset + ", string.length()" + imageString.length)
		}
		if (imageString != null) {
			val timestamp: String
			var delim: Int
			delim = imageString.indexOf("||", offset)
			filename = imageString.substring(offset, delim)
			offset = delim + 2
			delim = imageString.indexOf("||", offset)
			description = imageString.substring(offset, delim)
			offset = delim + 2
			timestamp = imageString.substring(offset)
			if (timestamp.length == 7) {
				time = Lyrics3TimeStamp("Time Stamp")
				time!!.readString(timestamp)
			}
		}
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		var str: String
		str = "filename = $filename, description = $description"
		if (time != null) {
			str += ", timestamp = " + time.toString()
		}
		return """
             $str

             """.trimIndent()
	}

	/**
	 * @return
	 */
	fun writeString(): String {
		var str: String?
		str = if (filename == null) {
			"||"
		} else {
			"$filename||"
		}
		str += if (description == null) {
			"||"
		} else {
			"$description||"
		}
		if (time != null) {
			str += time!!.writeString()
		}
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
