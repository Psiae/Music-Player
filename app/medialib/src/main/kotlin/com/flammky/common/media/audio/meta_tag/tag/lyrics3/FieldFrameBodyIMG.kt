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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidTagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.Lyrics3Image
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class FieldFrameBodyIMG : AbstractLyrics3v2FieldFrameBody {
	/**
	 *
	 */
	private var images = ArrayList<Lyrics3Image>()

	/**
	 * Creates a new FieldBodyIMG datatype.
	 */
	constructor()
	constructor(copyObject: FieldFrameBodyIMG) : super(copyObject) {
		var old: Lyrics3Image
		for (i in copyObject.images.indices) {
			old = copyObject.images[i]
			images.add(Lyrics3Image(old))
		}
	}

	/**
	 * Creates a new FieldBodyIMG datatype.
	 *
	 * @param imageString
	 */
	constructor(imageString: String) {
		readString(imageString)
	}

	/**
	 * Creates a new FieldBodyIMG datatype.
	 *
	 * @param image
	 */
	constructor(image: Lyrics3Image) {
		images.add(image)
	}

	/**
	 * Creates a new FieldBodyIMG datatype.
	 *
	 * @param byteBuffer
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer) {
		read(byteBuffer)
	}

	/**
	 * @return
	 */
	override val identifier: String
		get() = "IMG"// addField CRLF pair
	// cut off trailing crlf pair
	/**
	 * @return
	 */
	override val size: Int
		get() {
			var size = 0
			var image: Lyrics3Image
			for (image1 in images) {
				image = image1
				size += image.size + 2 // addField CRLF pair
			}
			return size - 2 // cut off trailing crlf pair
		}

	/**
	 * @param obj
	 * @return
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyIMG) {
			return false
		}
		val superset = obj.images
		for (image in images) {
			if (!superset.contains(image)) {
				return false
			}
		}
		return super.isSubsetOf(obj)
	}
	/**
	 * @return
	 */
	/**
	 * @param value
	 */
	var value: String
		get() = writeString()
		set(value) {
			readString(value)
		}

	/**
	 * @param image
	 */
	fun addImage(image: Lyrics3Image) {
		images.add(image)
	}

	/**
	 * @param obj
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyIMG) {
			return false
		}
		return images == obj.images && super.equals(obj)
	}

	/**
	 * @return
	 */
	override fun iterator(): Iterator<Lyrics3Image?>? {
		return images.iterator()
	}

	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val imageString: String
		var buffer = ByteArray(5)

		// read the 5 character size
		byteBuffer[buffer, 0, 5]
		val size = String(buffer, 0, 5).toInt()
		if (size == 0 && !TagOptionSingleton.instance.isLyrics3KeepEmptyFieldIfRead) {
			throw InvalidTagException("Lyircs3v2 Field has size of zero.")
		}
		buffer = ByteArray(size)

		// read the SIZE length description
		byteBuffer[buffer]
		imageString = String(buffer)
		readString(imageString)
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		var str = identifier + " : "
		for (image in images) {
			str += "$image ; "
		}
		return str
	}

	/**
	 * @param file
	 * @throws java.io.IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile) {
		val size: Int
		var offset = 0
		var buffer = ByteArray(5)
		var str: String
		size = this.size
		str = Integer.toString(size)
		for (i in 0 until 5 - str.length) {
			buffer[i] = '0'.code.toByte()
		}
		offset += 5 - str.length
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		offset += str.length
		file.write(buffer, 0, 5)
		if (size > 0) {
			str = writeString()
			buffer = ByteArray(str.length)
			for (i in 0 until str.length) {
				buffer[i] = str[i].code.toByte()
			}
			file.write(buffer)
		}
	}

	/**
	 * @param imageString
	 */
	private fun readString(imageString: String) {
		// now read each picture and put in the vector;
		var image: Lyrics3Image
		var token: String?
		var offset = 0
		var delim: Int = imageString.indexOf(Lyrics3v2Fields.Companion.CRLF)
		images = ArrayList()
		while (delim >= 0) {
			token = imageString.substring(offset, delim)
			image = Lyrics3Image("Image", this)
			image.setFilename(token)
			images.add(image)
			offset = delim + Lyrics3v2Fields.Companion.CRLF.length
			delim = imageString.indexOf(Lyrics3v2Fields.Companion.CRLF, offset)
		}
		if (offset < imageString.length) {
			token = imageString.substring(offset)
			image = Lyrics3Image("Image", this)
			image.setFilename(token)
			images.add(image)
		}
	}

	/**
	 * @return
	 */
	private fun writeString(): String {
		var str = ""
		var image: Lyrics3Image
		for (image1 in images) {
			image = image1
			str += image.writeString() + Lyrics3v2Fields.Companion.CRLF
		}
		return if (str.length > 2) {
			str.substring(0, str.length - 2)
		} else str
	}

	/**
	 * TODO
	 */
	override fun setupObjectList() {}
}
