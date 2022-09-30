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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import java.util.*
import java.util.logging.Logger

/**
 * Represents a field/data type that can be held within a frames body, these map loosely onto
 * Section 4. ID3v2 frame overview at http://www.id3.org/id3v2.4.0-structure.txt
 */

abstract class AbstractDataType {
	/**
	 * Get value held by this Object
	 *
	 * @return value held by this Object
	 */
	/**
	 * Set the value held by this datatype, this is used typically used when the
	 * user wants to modify the value in an existing frame.
	 *
	 * @param value
	 */
	/**
	 * Holds the data
	 */
	open var value: Any? = null

	/**
	 * Return the key as declared by the frame bodies datatype list
	 *
	 * @return the key used to reference this datatype from a framebody
	 */
	/**
	 * Holds the key such as "Text" or "PictureType", the naming of keys are fairly arbitary but are intended
	 * to make it easier to for the developer, the keys themseleves are not written to the tag.
	 */
	var identifier: String? = ""
		protected set
	/**
	 * Get the framebody associated with this datatype
	 *
	 * @return the framebody that this datatype is associated with
	 */
	/**
	 * Set the framebody that this datatype is associated with
	 *
	 * @param frameBody
	 */
	/**
	 * Holds the calling body, allows an datatype to query other objects in the
	 * body such as the Text Encoding of the frame
	 */
	open var body: AbstractTagFrameBody? = null

	/**
	 * Holds the size of the data in file when read/written
	 */
	open val size = 0

	/**
	 * Construct an abstract datatype identified by identifier and linked to a framebody without setting
	 * an initial value.
	 *
	 * @param identifier to allow retrieval of this datatype by name from framebody
	 * @param frameBody  that the dataype is associated with
	 */
	protected constructor(identifier: String?, frameBody: AbstractTagFrameBody?) {
		this.identifier = identifier
		body = frameBody
	}

	/**
	 * Construct an abstract datatype identified by identifier and linked to a framebody initilised with a value
	 *
	 * @param identifier to allow retrieval of this datatype by name from framebody
	 * @param frameBody  that the dataype is associated with
	 * @param value      of this DataType
	 */
	protected constructor(identifier: String?, frameBody: AbstractTagFrameBody?, value: Any?) {
		this.identifier = identifier
		body = frameBody
		this.value = value
	}

	/**
	 * This is used by subclasses, to clone the data within the copyObject
	 *
	 * TODO:It seems to be missing some of the more complex value types.
	 * @param copyObject
	 */
	constructor(copyObject: AbstractDataType?) {
		// no copy constructor in super class
		identifier = copyObject!!.identifier
		this.value = when (val copy = copyObject.value) {
			null -> null
			is String, is Boolean, is Byte, is Char, is Double, is Float, is Int, is Long, is Short,
			is MultipleTextEncodedStringNullTerminated.Values,
			is PairedTextEncodedStringNullTerminated.ValuePairs,
			is PartOfSet.PartOfSetValue -> copy
			is BooleanArray -> copy.clone()
			is ByteArray -> copy.clone()
			is CharArray -> copy.clone()
			is DoubleArray -> copy.clone()
			is FloatArray -> copy.clone()
			is IntArray -> copy.clone()
			is LongArray -> copy.clone()
			is ShortArray -> copy.clone()
			is Array<*> -> copy.clone()
			is ArrayList<*> -> copy.clone()
			is LinkedList<*> -> copy.clone()
			else -> {
				throw UnsupportedOperationException("Unable to create copy of class " + copyObject.javaClass)
			}
		}
	}

	/**
	 * Simplified wrapper for reading bytes from file into Object.
	 * Used for reading Strings, this class should be overridden
	 * for non String Objects
	 *
	 * @param arr
	 * @throws InvalidDataTypeException
	 */
	@Throws(InvalidDataTypeException::class)
	fun readByteArray(arr: ByteArray?) {
		readByteArray(arr, 0)
	}

	/**
	 * @param other
	 * @return whether this and obj are deemed equivalent
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is AbstractDataType) return false

		if (identifier != other.identifier) {
			return false
		}
		if (value == null && other.value == null) {
			return true
		} else if (value == null || other.value == null) {
			return false
		}
		// boolean[]
		if (value is BooleanArray && other.value is BooleanArray) {
			if (!Arrays.equals(value as BooleanArray?, other.value as BooleanArray?)) {
				return false
			}
			// byte[]
		} else if (value is ByteArray && other.value is ByteArray) {
			if (!Arrays.equals(value as ByteArray?, other.value as ByteArray?)) {
				return false
			}
			// char[]
		} else if (value is CharArray && other.value is CharArray) {
			if (!Arrays.equals(value as CharArray?, other.value as CharArray?)) {
				return false
			}
			// double[]
		} else if (value is DoubleArray && other.value is DoubleArray) {
			if (!Arrays.equals(value as DoubleArray?, other.value as DoubleArray?)) {
				return false
			}
			// float[]
		} else if (value is FloatArray && other.value is FloatArray) {
			if (!Arrays.equals(value as FloatArray?, other.value as FloatArray?)) {
				return false
			}
			// int[]
		} else if (value is IntArray && other.value is IntArray) {
			if (!Arrays.equals(value as IntArray?, other.value as IntArray?)) {
				return false
			}
			// long[]
		} else if (value is LongArray && other.value is LongArray) {
			if (!Arrays.equals(value as LongArray?, other.value as LongArray?)) {
				return false
			}
			// Object[]
		} else if (value is Array<*> && other.value is Array<*>) {
			if (!(value as Array<*>).contentEquals(other.value as Array<*>)) {
				return false
			}
			// short[]
		} else if (value is ShortArray && other.value is ShortArray) {
			if (!Arrays.equals(value as ShortArray?, other.value as ShortArray?)) {
				return false
			}
		} else if (value != other.value) {
			return false
		}
		return true
	}

	/**
	 * This is the starting point for reading bytes from the file into the ID3 datatype
	 * starting at offset.
	 * This class must be overridden
	 *
	 * @param arr
	 * @param offset
	 * @throws InvalidDataTypeException
	 */
	@Throws(InvalidDataTypeException::class)
	abstract fun readByteArray(arr: ByteArray?, offset: Int)

	/**
	 * Starting point write ID3 Datatype back to array of bytes.
	 * This class must be overridden.
	 *
	 * @return the array of bytes representing this datatype that should be written to file
	 */
	abstract fun writeByteArray(): ByteArray?

	/**
	 * Return String Representation of Datatype     *
	 */
	fun createStructure() {
		MP3File.structureFormatter?.addElement(identifier ?: return, value.toString())
	}

	companion object {
		protected const val TYPE_ELEMENT = "element"

		//Logger
		var logger = Logger.getLogger("org.jaudiotagger.tag.datatype")
	}
}
