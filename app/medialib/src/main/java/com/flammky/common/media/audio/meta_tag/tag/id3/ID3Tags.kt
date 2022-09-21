/*
 *  MusicTag Copyright (C)2003,2004
 *
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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.logging.Logger

/**
 * This contains static methods that can be performed on tags
 * and to convert between tags.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
object ID3Tags {
	//Logger
	var logger = Logger.getLogger("org.jaudiotagger.tag.id3")

	/**
	 * Returns true if the identifier is a valid ID3v2.2 frame identifier
	 *
	 * @param identifier string to test
	 * @return true if the identifier is a valid ID3v2.2 frame identifier
	 */
	@JvmStatic
	fun isID3v22FrameIdentifier(identifier: String): Boolean {
		//If less than 3 cant be an identifier
		return if (identifier.length < 3) {
			false
		} else identifier.length == 3 && ID3v22Frames.instanceOf.getIdToValueMap().containsKey(
			identifier
		)
	}

	/**
	 * Returns true if the identifier is a valid ID3v2.3 frame identifier
	 *
	 * @param identifier string to test
	 * @return true if the identifier is a valid ID3v2.3 frame identifier
	 */
	@JvmStatic
	fun isID3v23FrameIdentifier(identifier: String): Boolean {
		return identifier.length >= 4 && ID3v23Frames.instanceOf.getIdToValueMap().containsKey(
			identifier.substring(0, 4)
		)
	}

	/**
	 * Returns true if the identifier is a valid ID3v2.4 frame identifier
	 *
	 * @param identifier string to test
	 * @return true if the identifier is a valid ID3v2.4 frame identifier
	 */
	@JvmStatic
	fun isID3v24FrameIdentifier(identifier: String): Boolean {
		return identifier.length >= 4 && ID3v24Frames.instanceOf.getIdToValueMap().containsKey(
			identifier.substring(0, 4)
		)
	}

	/**
	 * Given an datatype, try to return it as a `long`. This tries to
	 * parse a string, and takes `Long, Short, Byte, Integer`
	 * objects and gets their value. An exception is not explicitly thrown
	 * here because it would causes too many other methods to also throw it.
	 *
	 * @param value datatype to find long from.
	 * @return `long` value
	 * @throws IllegalArgumentException
	 */
	@JvmStatic
	fun getWholeNumber(value: Any): Long {
		val number: Long = when (value) {
			is Long -> value
			is String -> value.toLong()
			is Byte -> value.toLong()
			is Short -> value.toLong()
			is Int -> value.toLong()
			else -> throw IllegalArgumentException("Unsupported value class: " + value.javaClass.name)
		}
		return number
	}

	/**
	 * Convert from ID3v22 FrameIdentifier to ID3v23
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun convertFrameID22To23(identifier: String): String? {
		return if (identifier.length < 3) {
			null
		} else ID3Frames.convertv22Tov23[identifier.subSequence(0, 3) as String]
	}

	/**
	 * Convert from ID3v22 FrameIdentifier to ID3v24
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun convertFrameID22To24(identifier: String): String? {
		//Idv22 identifiers are only of length 3 times
		if (identifier.length < 3) {
			return null
		}
		//Has idv22 been mapped to v23
		val id = ID3Frames.convertv22Tov23[identifier.substring(0, 3)]
		return if (id != null) {
			//has v2.3 been mapped to v2.4
			val v23id =
				ID3Frames.convertv23Tov24[id]
			v23id
				?: //if not it may be because v2.3 and and v2.4 are same so wont be
				//in mapping
				if (ID3v24Frames.instanceOf.getIdToValueMap()[id] != null
				) {
					id
				} else {
					null
				}
		} else {
			null
		}
	}

	/**
	 * Convert from ID3v23 FrameIdentifier to ID3v22
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun convertFrameID23To22(identifier: String): String? {
		if (identifier.length < 4) {
			return null
		}

		//If it is a v23 identifier
		return if (ID3v23Frames.instanceOf.getIdToValueMap().containsKey(
				identifier
			)
		) {
			//If only name has changed  v22 and modified in v23 return result of.
			ID3Frames.convertv23Tov22[identifier.substring(0, 4)]
		} else null
	}

	/**
	 * Convert from ID3v23 FrameIdentifier to ID3v24
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun convertFrameID23To24(identifier: String): String? {
		if (identifier.length < 4) {
			return null
		}

		//If it is a ID3v23 identifier
		return if (ID3v23Frames.instanceOf.getIdToValueMap().containsKey(
				identifier
			)
		) {
			//If no change between ID3v23 and ID3v24 should be in ID3v24 list.
			if (ID3v24Frames.instanceOf.getIdToValueMap().containsKey(
					identifier
				)
			) {
				identifier
			} else {
				ID3Frames.convertv23Tov24[identifier.substring(0, 4)]
			}
		} else null
	}

	/**
	 * Force from ID3v22 FrameIdentifier to ID3v23, this is where the frame and structure
	 * has changed from v2 to v3 but we can still do some kind of conversion.
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun forceFrameID22To23(identifier: String?): String? {
		return ID3Frames.forcev22Tov23[identifier]
	}

	/**
	 * Force from ID3v22 FrameIdentifier to ID3v23, this is where the frame and structure
	 * has changed from v2 to v3 but we can still do some kind of conversion.
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun forceFrameID23To22(identifier: String?): String? {
		return ID3Frames.forcev23Tov22[identifier]
	}

	/**
	 * Force from ID3v2.30 FrameIdentifier to ID3v2.40, this is where the frame and structure
	 * has changed from v3 to v4 but we can still do some kind of conversion.
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun forceFrameID23To24(identifier: String?): String? {
		return ID3Frames.forcev23Tov24[identifier]
	}

	/**
	 * Force from ID3v2.40 FrameIdentifier to ID3v2.30, this is where the frame and structure
	 * has changed between v4 to v3 but we can still do some kind of conversion.
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun forceFrameID24To23(identifier: String?): String? {
		return ID3Frames.forcev24Tov23[identifier]
	}

	/**
	 * Convert from ID3v24 FrameIdentifier to ID3v23
	 * @param identifier
	 * @return
	 */
	@JvmStatic
	fun convertFrameID24To23(identifier: String): String? {
		var id: String?
		if (identifier.length < 4) {
			return null
		}
		id = ID3Frames.convertv24Tov23[identifier]
		if (id == null) {
			if (ID3v23Frames.instanceOf.getIdToValueMap().containsKey(identifier)) {
				id = identifier
			}
		}
		return id
	}

	/**
	 * Unable to instantiate abstract classes, so can't call the copy
	 * constructor. So find out the instantiated class name and call the copy
	 * constructor through reflection (e.g for a a FrameBody would have to have a constructor
	 * that takes another frameBody as the same type as a parameter)
	 *
	 * @param copyObject
	 * @return
	 * @throws IllegalArgumentException if no suitable constructor exists
	 */
	@JvmStatic
	fun copyObject(copyObject: Any?): Any? {
		val constructor: Constructor<*>
		val constructorParameterArray: Array<Class<*>?>
		val parameterArray: Array<Any?>
		return if (copyObject == null) {
			null
		} else try {
			constructorParameterArray = arrayOfNulls(1)
			constructorParameterArray[0] = copyObject.javaClass
			constructor = copyObject.javaClass.getConstructor(*constructorParameterArray)
			parameterArray = arrayOfNulls(1)
			parameterArray[0] = copyObject
			constructor.newInstance(*parameterArray)
		} catch (ex: NoSuchMethodException) {
			throw IllegalArgumentException("NoSuchMethodException: Error finding constructor to create copy:" + copyObject.javaClass.name)
		} catch (ex: IllegalAccessException) {
			throw IllegalArgumentException("IllegalAccessException: No access to run constructor to create copy" + copyObject.javaClass.name)
		} catch (ex: InstantiationException) {
			throw IllegalArgumentException("InstantiationException: Unable to instantiate constructor to copy" + copyObject.javaClass.name)
		} catch (ex: InvocationTargetException) {
			if (ex.cause is Error) {
				throw (ex.cause as Error?)!!
			} else if (ex.cause is RuntimeException) {
				throw (ex.cause as RuntimeException?)!!
			} else {
				throw IllegalArgumentException("InvocationTargetException: Unable to invoke constructor to create copy")
			}
		}
	}
	/**
	 * Find the first whole number that can be parsed from the string
	 *
	 * @param str    string to search
	 * @param offset start seaching from this index
	 * @return first whole number that can be parsed from the string
	 * @throws TagException
	 * @throws NullPointerException
	 * @throws IndexOutOfBoundsException
	 */
	/**
	 * Find the first whole number that can be parsed from the string
	 *
	 * @param str string to search
	 * @return first whole number that can be parsed from the string
	 * @throws TagException
	 */
	@JvmOverloads
	@Throws(TagException::class)
	fun findNumber(str: String?, offset: Int = 0): Long {
		if (str == null) {
			throw NullPointerException("String is null")
		}
		if (offset < 0 || offset >= str.length) {
			throw IndexOutOfBoundsException("Offset to image string is out of bounds: offset = " + offset + ", string.length()" + str.length)
		}
		var i: Int
		var j: Int
		val num: Long
		i = offset
		while (i < str.length) {
			if (str[i] >= '0' && str[i] <= '9' || str[i] == '-') {
				break
			}
			i++
		}
		j = i + 1
		while (j < str.length) {
			if (str[j] < '0' || str[j] > '9') {
				break
			}
			j++
		}
		num = if (j <= str.length && j > i) {
			val toParseNumberFrom = str.substring(i, j)
			if (toParseNumberFrom != "-") toParseNumberFrom.toLong() else throw TagException(
				"Unable to find integer in string: $str"
			)
		} else {
			throw TagException("Unable to find integer in string: $str")
		}
		return num
	}

	/**
	 * Remove all occurances of the given character from the string argument.
	 *
	 * @param str String to search
	 * @param ch  character to remove
	 * @return new String without the given charcter
	 */
	@JvmStatic
	fun stripChar(str: String?, ch: Char): String? {
		return if (str != null) {
			val buffer = CharArray(str.length)
			var next = 0
			for (i in 0 until str.length) {
				if (str[i] != ch) {
					buffer[next++] = str[i]
				}
			}
			String(buffer, 0, next)
		} else {
			null
		}
	}

	/**
	 * truncate a string if it longer than the argument
	 *
	 * @param str String to truncate
	 * @param len maximum desired length of new string
	 * @return
	 */
	@JvmStatic
	fun truncate(str: String?, len: Int): String? {
		if (str == null) {
			return null
		}
		if (len < 0) {
			return null
		}
		return if (str.length > len) {
			str.substring(0, len)
		} else {
			str
		}
	}
}
