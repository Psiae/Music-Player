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
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.*

class FieldFrameBodyUnsupported : AbstractLyrics3v2FieldFrameBody {
	/**
	 *
	 */
	private var value: ByteArray? = null

	/**
	 * Creates a new FieldBodyUnsupported datatype.
	 */
	constructor() {
		//        this.value = new byte[0];
	}

	constructor(copyObject: FieldFrameBodyUnsupported) : super(copyObject) {
		value = copyObject.value!!.clone()
	}

	/**
	 * Creates a new FieldBodyUnsupported datatype.
	 *
	 * @param value
	 */
	constructor(value: ByteArray) {
		this.value = value
	}

	/**
	 * Creates a new FieldBodyUnsupported datatype.
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
		get() = "ZZZ"

	/**
	 * @param obj
	 * @return
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyUnsupported) {
			return false
		}
		val subset = String(value!!)
		val superset = String(obj.value!!)
		return superset.contains(subset) && super.isSubsetOf(obj)
	}

	/**
	 * @param obj
	 * @return
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is FieldFrameBodyUnsupported) {
			return false
		}
		return Arrays.equals(value, obj.value) && super.equals(obj)
	}

	/**
	 * @param byteBuffer
	 * @throws IOException
	 */
	@Throws(InvalidTagException::class)
	override fun read(byteBuffer: ByteBuffer) {
		val size: Int
		val buffer = ByteArray(5)

		// read the 5 character size
		byteBuffer[buffer, 0, 5]
		size = String(buffer, 0, 5).toInt()
		value = ByteArray(size)

		// read the SIZE length description
		byteBuffer[value]
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return identifier + " : " + String(value!!)
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile) {
		var offset = 0
		val str: String
		val buffer = ByteArray(5)
		str = Integer.toString(value!!.size)
		for (i in 0 until 5 - str.length) {
			buffer[i] = '0'.code.toByte()
		}
		offset += 5 - str.length
		for (i in 0 until str.length) {
			buffer[i + offset] = str[i].code.toByte()
		}
		file.write(buffer)
		file.write(value)
	}

	/**
	 * TODO
	 */
	override fun setupObjectList() {}
}
