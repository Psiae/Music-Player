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
 * This abstract class defines methods for writing out the contents of a tag in a user-friendly way
 * Concrete subclasses could implement different versions such as XML Output, PDF and so on. The tag
 * in all cases is diaplyed as a sort of tree hierachy.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.logging

/**
 * Abstract class that provides structure to use for displaying a files metadata content
 */
abstract class AbstractTagDisplayFormatter {
	protected var level = 0
	abstract fun openHeadingElement(type: String, value: String)
	abstract fun openHeadingElement(type: String, value: Boolean)
	abstract fun openHeadingElement(type: String, value: Int)
	abstract fun closeHeadingElement(type: String)
	abstract fun addElement(type: String, value: String?)
	abstract fun addElement(type: String, value: Int)
	abstract fun addElement(type: String, value: Boolean)
	abstract override fun toString(): String

	companion object {
		private val hexBinaryMap = HashMap<String, String>()

		/**
		 * Use to display headers as their binary representation
		 * @param buffer
		 * @return
		 */
		fun displayAsBinary(buffer: Byte): String {
			//Convert buffer to hex representation
			val hexValue = Integer.toHexString(buffer.toInt())
			var char1 = ""
			var char2 = ""
			try {
				when (hexValue.length) {
					8 -> {
						char1 = hexValue.substring(6, 7)
						char2 = hexValue.substring(7, 8)
					}
					2 -> {
						char1 = hexValue.substring(0, 1)
						char2 = hexValue.substring(1, 2)
					}
					1 -> {
						char1 = "0"
						char2 = hexValue.substring(0, 1)
					}
				}
			} catch (se: StringIndexOutOfBoundsException) {
				return ""
			}
			return hexBinaryMap[char1] + hexBinaryMap[char2]
		}

		init {
			hexBinaryMap["0"] = "0000"
			hexBinaryMap["1"] = "0001"
			hexBinaryMap["2"] = "0010"
			hexBinaryMap["3"] = "0011"
			hexBinaryMap["4"] = "0100"
			hexBinaryMap["5"] = "0101"
			hexBinaryMap["6"] = "0110"
			hexBinaryMap["7"] = "0111"
			hexBinaryMap["8"] = "1000"
			hexBinaryMap["9"] = "1001"
			hexBinaryMap["a"] = "1010"
			hexBinaryMap["b"] = "1011"
			hexBinaryMap["c"] = "1100"
			hexBinaryMap["d"] = "1101"
			hexBinaryMap["e"] = "1110"
			hexBinaryMap["f"] = "1111"
		}
	}
}
