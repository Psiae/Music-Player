/*
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 *
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.util.*
import java.util.logging.Level

/**
 * Represents a list of [Cloneable](!!) [AbstractDataType]s, continuing until the end of the buffer.
 *
 * @author [Hendrik Schreiber](mailto:hs@tagtraum.com)
 * @version $Id:$
 */

/** [org.jaudiotagger.tag.datatype.AbstractDataTypeList] */
abstract class AbstractDataTypeList<T : AbstractDataType> : AbstractDataType {

	open var valueList: MutableList<T> = mutableListOf()

	override var value: Any?
		@Suppress("UNCHECKED_CAST")
		get() = valueList
		set(value) {
			valueList = when {
				value == null || (value is List<*> && value.isEmpty()) -> ArrayList()
				else -> throw IllegalArgumentException()
			}
		}

	override val size: Int
		get() {
			var size = 0
			valueList.forEach { size += it.size }
			return size
		}

	constructor(identifier: String, frameBody: AbstractTagFrameBody) : super(identifier, frameBody) {
		valueList = ArrayList()
	}

	/**
	 * Copy constructor.
	 * By convention, subclasses *must* implement a constructor, accepting an argument of their own class type
	 * and call this constructor for [ID3Tags.copyObject] to work.
	 * A parametrized `AbstractDataTypeList` is not sufficient.
	 *
	 * @param copy instance
	 */
	protected constructor(copy: AbstractDataTypeList<T>?) : super(copy)


	/**
	 * Reads list of [EventTimingCode]s from buffer starting at the given offset.
	 *
	 * @param buffer buffer
	 * @param offset initial offset into the buffer
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(buffer: ByteArray?, offset: Int) {
		requireNotNull(buffer) {
			"Buffer was Null"
		}
		require(offset > 0) {
			"Offset to byte array is out of bounds: offset = " + offset + ", array.length = " + buffer.size
		}

		// no events
		if (offset >= buffer.size) {
			valueList.clear()
			return
		}

		var currentOffset = offset
		while (currentOffset < buffer.size) {
			val data = createListElement()
			data.readByteArray(buffer, currentOffset)
			data.body = body
			valueList.add(data)
			currentOffset += data.size
		}
	}

	/**
	 * Factory method that creates new elements for this list.
	 * Called from [.readByteArray].
	 *
	 * @return new list element
	 */
	protected abstract fun createListElement(): T

	/**
	 * Write contents to a byte array.
	 *
	 * @return a byte array that that contains the data that should be persisted to file
	 */
	override fun writeByteArray(): ByteArray? {
		if (logger.isLoggable(Level.CONFIG)) {
			logger.config("Writing DataTypeList " + identifier)
		}
		val buffer = ByteArray(size)
		var offset = 0
		for (data in valueList) {
			val bytes = data.writeByteArray()
			System.arraycopy(bytes, 0, buffer, offset, bytes!!.size)
			offset += bytes.size
		}
		return buffer
	}

	override fun hashCode(): Int = Objects.hash(value, size, super.hashCode())

	override fun toString(): String = value?.toString() ?: ""
}
