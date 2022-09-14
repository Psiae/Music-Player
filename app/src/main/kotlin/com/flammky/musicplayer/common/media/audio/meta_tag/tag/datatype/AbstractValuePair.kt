/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 *
 * Jaudiotagger Copyright (C)2004,2005
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

/**
 * A two way mapping between an id and a value
 */
abstract class AbstractValuePair<I, V> {
	@JvmField
	protected val idToValue: MutableMap<I, V> = LinkedHashMap()
	protected val valueToId: MutableMap<V, I> = LinkedHashMap()
	protected val valueList: MutableList<V> = ArrayList()
	protected var iterator = idToValue.keys.iterator()
	protected var value: String? = null

	/**
	 * Get list in alphabetical order
	 * @return
	 */
	fun getAlphabeticalValueList(): MutableList<V> {
		return valueList
	}

	fun getIdToValueMap(): Map<I, V> {
		return idToValue
	}

	fun getValueToIdMap(): Map<V, I> {
		return valueToId
	}

	/**
	 * @return the number of elements in the mapping
	 */
	fun getSize(): Int {
		return valueList.size
	}
}
