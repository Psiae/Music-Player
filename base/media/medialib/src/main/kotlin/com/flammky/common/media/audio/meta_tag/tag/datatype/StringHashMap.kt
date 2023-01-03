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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.AbstractTagFrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Represents a String thats acts as a key into an enumeration of values. The String will be encoded
 * using the default encoding regardless of what encoding may be specified in the framebody
 */
class StringHashMap : StringFixedLength, HashMapInterface<String, String> {
	/**
	 *
	 */
	private var keyToValue: Map<String, String>? = null

	/**
	 *
	 */
	private var valueToKey: Map<String, String>? = null

	/**
	 *
	 */
	var hasEmptyValue = false


	override var value: Any? = super.value
		set(value) {
			field =
				if (value is String) {
					//Issue #273 temporary hack for MM
					if (value == "XXX") {
						value.toString()
					} else {
						value.lowercase(Locale.getDefault())
					}
				} else {
					value
				}
		}

	/**
	 * Creates a new ObjectStringHashMap datatype.
	 *
	 * @param identifier
	 * @param frameBody
	 * @param size
	 * @throws IllegalArgumentException
	 */
	constructor(identifier: String, frameBody: AbstractTagFrameBody?, size: Int) : super(
		identifier,
		frameBody,
		size
	) {
		if (identifier == DataTypes.OBJ_LANGUAGE) {
			valueToKey = Languages.instanceOf.getValueToIdMap()
			keyToValue = Languages.instanceOf.getIdToValueMap()
		} else {
			throw IllegalArgumentException("Hashmap identifier not defined in this class: $identifier")
		}
	}

	constructor(copyObject: StringHashMap) : super(copyObject) {
		hasEmptyValue = copyObject.hasEmptyValue
		keyToValue = copyObject.keyToValue
		valueToKey = copyObject.valueToKey
	}

	/**
	 * @return
	 */
	override fun getKeyToValue(): Map<String, String>? {
		return keyToValue
	}

	/**
	 * @return
	 */
	override fun getValueToKey(): Map<String, String>? {
		return valueToKey
	}

	/**
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (other !is StringHashMap) {
			return false
		}
		val `object` = other
		if (hasEmptyValue != `object`.hasEmptyValue) {
			return false
		}
		if (keyToValue == null) {
			if (`object`.keyToValue != null) {
				return false
			}
		} else {
			if (keyToValue != `object`.keyToValue) {
				return false
			}
		}
		if (keyToValue == null) {
			if (`object`.keyToValue != null) {
				return false
			}
		} else {
			if (valueToKey != `object`.valueToKey) {
				return false
			}
		}
		return super.equals(other)
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
	 * @return
	 */
	override fun toString(): String {
		return if (value == null || keyToValue!![value] == null) {
			""
		} else {
			keyToValue!!.get(value)!!
		}
	}

	override val textEncodingCharSet: Charset?
		get() = StandardCharsets.ISO_8859_1

}
