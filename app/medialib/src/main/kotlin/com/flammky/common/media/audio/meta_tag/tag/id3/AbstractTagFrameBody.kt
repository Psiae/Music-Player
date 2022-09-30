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
 * FragmentBody contains the data for a fragment.
 * ID3v2 tags have frames bodys. Lyrics3 tags have fields bodys
 * ID3v1 tags do not have fragments bodys.
 * Fragment Bodies consist of a number of MP3Objects held in an objectList
 * Methods are additionally defined here to restrieve and set these objects.
 * We also specify methods for getting/setting the text encoding of textual
 * data.
 * Fragment bodies should not be concerned about their parent fragment. For
 * example most ID3v2 frames can be applied to ID3v2tags of different versions.
 * The frame header will need modification based on the frame version but this
 * should have no effect on the frame body.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractDataType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3Tags.copyObject
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding

/**
 * A frame body contains the data content for a frame
 */
abstract class AbstractTagFrameBody : AbstractTagItem {
	open fun createStructure() {}
	/**
	 * Get Reference to header
	 *
	 * @return
	 */
	/**
	 * Set header
	 *
	 * @param header
	 */
	/**
	 * Reference to the header associated with this frame body, a framebody can be created without a header
	 * but one it is associated with a header this should be set. It is principally useful for the framebody to know
	 * its header, because this will specify its tag version and some framebodies behave slighly different
	 * between tag versions.
	 */
	var header: AbstractTagFrame? = null

	/**
	 * List of data types that make up this particular frame body.
	 */
	@JvmField
	protected var objectList: MutableList<AbstractDataType?> = ArrayList()
	/**
	 * Return the Text Encoding
	 *
	 * @return the text encoding used by this framebody
	 *///Number HashMap actually converts this byte to a long
	/**
	 * Set the Text Encoding to use for this frame body
	 *
	 * @param textEncoding to use for this frame body
	 */
	var textEncoding: Byte
		get() {
			val o = getObject(DataTypes.OBJ_TEXT_ENCODING)
			return if (o != null) {
				val encoding = o.value as Long
				encoding.toByte()
			} else {
				TextEncoding.ISO_8859_1
			}
		}
		set(textEncoding) {
			//Number HashMap actually converts this byte to a long
			setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		}

	/**
	 * Creates a new framebody, at this point the bodys
	 * ObjectList is setup which defines what datatypes are expected in body
	 */
	protected constructor() {
		setupObjectList()
	}

	/**
	 * Copy Constructor for fragment body. Copies all objects in the
	 * Object Iterator with data.
	 * @param copyObject
	 */
	protected constructor(copyObject: AbstractTagFrameBody) {
		for (i in copyObject.objectList.indices) {
			val newObject = copyObject(copyObject.objectList[i]) as AbstractDataType
			newObject.body = this
			objectList.add(newObject)
		}
	}

	/**
	 *
	 * @return the text value that the user would expect to see for this framebody type, this should be overridden
	 * for all frame-bodies
	 */
	open val userFriendlyValue: String?
		get() = toString()

	/**
	 * This method calls `toString` for all it's objects and appends
	 * them without any newline characters.
	 *
	 * @return brief description string
	 */
	open val briefDescription: String
		get() {
			var str = ""
			for (`object` in objectList) {
				if (`object`.toString() != null && `object`.toString().length > 0) {
					str += `object`!!.identifier + "=\"" + `object`.toString() + "\"; "
				}
			}
			return str
		}

	/**
	 * This method calls `toString` for all it's objects and appends
	 * them. It contains new line characters and is more suited for display
	 * purposes
	 *
	 * @return formatted description string
	 */
	val longDescription: String
		get() {
			var str = ""
			for (`object` in objectList) {
				if (`object`.toString() != null && `object`.toString().length > 0) {
					str += `object`!!.identifier + " = " + `object`.toString() + "\n"
				}
			}
			return str
		}

	/**
	 * Sets all objects of identifier type to value defined by `obj` argument.
	 *
	 * @param identifier `MP3Object` identifier
	 * @param value      new datatype value
	 */
	fun setObjectValue(identifier: String, value: Any?) {
		for (`object` in objectList) {
			if (`object`!!.identifier == identifier) {
				`object`.value = value
			}
		}
	}

	/**
	 * Returns the value of the datatype with the specified
	 * `identifier`
	 *
	 * @param identifier
	 * @return the value of the dattype with the specified
	 * `identifier`
	 */
	fun getObjectValue(identifier: String): Any? {
		return getObject(identifier)?.value
	}

	/**
	 * Returns the datatype with the specified
	 * `identifier`
	 *
	 * @param identifier
	 * @return the datatype with the specified
	 * `identifier`
	 */
	fun getObject(identifier: String): AbstractDataType? {
		for (`object` in objectList) {
			if (`object`!!.identifier == identifier) {
				return `object`
			}
		}
		return null
	}

	/**
	 * Returns the size in bytes of this fragmentbody
	 *
	 * @return estimated size in bytes of this datatype
	 */
	override val size: Int
		get() {
			var size = 0
			for (`object` in objectList) {
				size += `object`!!.size
			}
			return size
		}

	/**
	 * Returns true if this instance and its entire DataType
	 * array list is a subset of the argument. This class is a subset if it is
	 * the same class as the argument.
	 *
	 * @param obj datatype to determine subset of
	 * @return true if this instance and its entire datatype array list is a
	 * subset of the argument.
	 */
	override fun isSubsetOf(obj: Any?): Boolean {
		if (obj !is AbstractTagFrameBody) {
			return false
		}
		val superset: List<AbstractDataType?> = obj.objectList
		for (`object` in objectList) {
			if (`object`!!.value != null) {
				if (!superset.contains(`object`)) {
					return false
				}
			}
		}
		return true
	}

	/**
	 * Returns true if this datatype and its entire DataType array
	 * list equals the argument. This datatype is equal to the argument if they
	 * are the same class.
	 *
	 * @param obj datatype to determine equality of
	 * @return true if this datatype and its entire `MP3Object` array
	 * list equals the argument.
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is AbstractTagFrameBody) {
			return false
		}
		return objectList == obj.objectList && super.equals(obj)
	}

	/**
	 * Returns an iterator of the DataType list.
	 *
	 * @return iterator of the DataType list.
	 */
	open operator fun iterator(): Iterator<AbstractDataType?>? {
		return objectList.iterator()
	}

	/**
	 * Return brief description of FrameBody
	 *
	 * @return brief description of FrameBody
	 */
	override fun toString(): String {
		return briefDescription
	}

	/**
	 * Create the list of Datatypes that this body
	 * expects in the correct order This method needs to be implemented by concrete subclasses
	 */
	protected abstract fun setupObjectList()
}
