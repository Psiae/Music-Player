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

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.InvalidDataTypeException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.EqualsUtil.areEqual
import java.util.*

/**
 * Represents a number thats acts as a key into an enumeration of values
 */
class NumberHashMap : NumberFixedLength, HashMapInterface<Int, String> {
	/**
	 * key to value map
	 */
	private var keyToValue: Map<Int, String>? = null

	/**
	 * value to key map
	 */
	private var valueToKey: Map<String, Int>? = null

	/**
	 *
	 */
	private var hasEmptyValue = false

	/**
	 * Creates a new ObjectNumberHashMap datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param size
	 * @throws IllegalArgumentException
	 */
	constructor(
		identifier: String?,
		frameBody: AbstractTagFrameBody?,
		size: Int
	) : super(identifier, frameBody, size) {
		if (identifier == DataTypes.OBJ_GENRE) {
			valueToKey = GenreTypes.instanceOf.getValueToIdMap()
			keyToValue = GenreTypes.instanceOf.getIdToValueMap()

			//genres can be an id or literal value
			hasEmptyValue = true
		} else if (identifier == DataTypes.OBJ_TEXT_ENCODING) {
			valueToKey = TextEncoding.instanceOf.getValueToIdMap()
			keyToValue = TextEncoding.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_INTERPOLATION_METHOD) {
			valueToKey = InterpolationTypes.instanceOf.getValueToIdMap()
			keyToValue = InterpolationTypes.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_PICTURE_TYPE) {
			valueToKey = PictureTypes.instanceOf.getValueToIdMap()
			keyToValue = PictureTypes.instanceOf.getIdToValueMap()

			//Issue #224 Values should map, but have examples where they dont, this is a workaround
			hasEmptyValue = true
		} else if (identifier == DataTypes.OBJ_TYPE_OF_EVENT) {
			valueToKey = EventTimingTypes.instanceOf.getValueToIdMap()
			keyToValue = EventTimingTypes.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_TIME_STAMP_FORMAT) {
			valueToKey = EventTimingTimestampTypes.instanceOf.getValueToIdMap()
			keyToValue = EventTimingTimestampTypes.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_TYPE_OF_CHANNEL) {
			valueToKey = ChannelTypes.instanceOf.getValueToIdMap()
			keyToValue = ChannelTypes.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_RECIEVED_AS) {
			valueToKey = ReceivedAsTypes.instanceOf.getValueToIdMap()
			keyToValue = ReceivedAsTypes.instanceOf.getIdToValueMap()
		} else if (identifier == DataTypes.OBJ_CONTENT_TYPE) {
			valueToKey = SynchronisedLyricsContentType.instanceOf.getValueToIdMap()
			keyToValue = SynchronisedLyricsContentType.instanceOf.getIdToValueMap()
		} else {
			throw IllegalArgumentException("Hashmap identifier not defined in this class: $identifier")
		}
	}

	constructor(copyObject: NumberHashMap) : super(copyObject) {
		hasEmptyValue = copyObject.hasEmptyValue

		// we don't need to clone/copy the maps here because they are static
		keyToValue = copyObject.keyToValue
		valueToKey = copyObject.valueToKey
	}

	/**
	 * @return the key to value map
	 */
	override fun getKeyToValue(): Map<Int, String>? {
		return keyToValue
	}

	/**
	 * @return the value to key map
	 */
	override fun getValueToKey(): Map<String, Int>? {
		return valueToKey
	}

	override var value: Any? = super.value
		set(value) {
			field = when (value) {
				is Byte -> value.toLong()
				is Short -> value.toLong()
				is Int -> value.toLong()
				else -> value
			}
		}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other === this) {
			return true
		}
		if (other !is NumberHashMap) {
			return false
		}
		val that = other
		return areEqual(hasEmptyValue, that.hasEmptyValue) &&
			areEqual(keyToValue, that.keyToValue) &&
			areEqual(valueToKey, that.valueToKey) &&
			super.equals(that)
	}

	/**
	 * @return
	 */
	override fun iterator(): Iterator<String>? {
		return if (keyToValue == null) {
			null
		} else {
			// put them in a treeset first to sort them
			val treeSet = TreeSet(
				keyToValue!!.values
			)
			if (hasEmptyValue) {
				treeSet.add("")
			}
			treeSet.iterator()
		}
	}

	/**
	 * Read the key from the buffer.
	 *
	 * @param arr
	 * @param offset
	 * @throws InvalidDataTypeException if emptyValues are not allowed and the eky was invalid.
	 */
	@Throws(InvalidDataTypeException::class)
	override fun readByteArray(arr: ByteArray?, offset: Int) {
		super.readByteArray(arr, offset)

		//Mismatch:Superclass uses Long, but maps expect Integer
		val intValue = (value as Long).toInt()
		if (!keyToValue!!.containsKey(intValue)) {
			if (!hasEmptyValue) {
				throw InvalidDataTypeException(
					ErrorMessage.MP3_REFERENCE_KEY_INVALID.getMsg(
						identifier,
						intValue
					)
				)
			} else if (identifier == DataTypes.OBJ_PICTURE_TYPE) {
				logger.warning(
					ErrorMessage.MP3_PICTURE_TYPE_INVALID.getMsg(
						value
					)
				)
			}
		}
	}

	/**
	 * @return
	 */
	override fun toString(): String {
		return if (value == null) {
			""
		} else if (keyToValue!![value] == null) {
			""
		} else {
			keyToValue!!.get(value)!!
		}
	}
}
