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
 * A two way mapping between an Integral Id and a String value
 */
open class AbstractIntStringValuePair : AbstractValuePair<Int, String>() {
	protected var key: Int? = null

	/**
	 * Get Id for Value
	 * @param value
	 * @return
	 */
	fun getIdForValue(value: String): Int? {
		return valueToId[value]
	}

	/**
	 * Get value for Id
	 * @param id
	 * @return
	 */
	fun getValueForId(id: Int): String? {
		return idToValue[id]
	}

	protected fun createMaps() {
		//Create the reverse the map
		for ((key, value1) in idToValue) {
			valueToId[value1] = key
		}

		//Value List sort alphabetically
		valueList.addAll(idToValue.values)
		valueList.sort()
	}
}
