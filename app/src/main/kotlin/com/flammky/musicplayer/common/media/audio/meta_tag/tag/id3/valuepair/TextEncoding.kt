/**
 * @author : Paul Taylor
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
 * Valid Text Encodings
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractIntStringValuePair
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Text Encoding supported by ID3v24, the id is recognised by ID3
 * whereas the value maps to a java java.nio.charset.Charset, all the
 * charsets defined below are guaranteed on every Java platform.
 *
 * Note in ID3 UTF_16 can be implemented as either UTF16BE or UTF16LE with byte ordering
 * marks, in JAudioTagger we always implement it as UTF16LE because only this order
 * is understood in Windows, OSX seem to understand both.
 */
class TextEncoding private constructor() : AbstractIntStringValuePair() {
	private val idToCharset: MutableMap<Int, Charset> = HashMap()

	init {
		idToCharset[ISO_8859_1.toInt()] = StandardCharsets.ISO_8859_1
		idToCharset[UTF_16.toInt()] = StandardCharsets.UTF_16
		idToCharset[UTF_16BE.toInt()] = StandardCharsets.UTF_16BE
		idToCharset[UTF_8.toInt()] = StandardCharsets.UTF_8
		for ((key, value) in idToCharset) {
			idToValue[key] = value.name()
		}
		createMaps()
	}

	/**
	 * Allows to lookup id directly via the [Charset] instance.
	 *
	 * @param charset charset
	 * @return id, e.g. [.ISO_8859_1], or `null`, if not found
	 */
	fun getIdForCharset(charset: Charset?): Int? {
		if (charset == null) return null
		return valueToId[charset.name()]
	}

	/**
	 * Allows direct lookup of the [Charset] instance via an id.
	 *
	 * @param id id, e.g. [.ISO_8859_1]
	 * @return charset or `null`, if not found
	 */
	fun getCharsetForId(id: Int): Charset? {
		return idToCharset[id]
	}

	companion object {
		//Supported ID3 charset ids
		const val ISO_8859_1: Byte = 0
		const val UTF_16: Byte =
			1 //We use UTF-16 with LE byte-ordering and byte order mark by default

		//but can also use BOM with BE byte ordering
		const val UTF_16BE: Byte = 2
		const val UTF_8: Byte = 3

		/** The number of bytes used to hold the text encoding field size.  */
		const val TEXT_ENCODING_FIELD_SIZE = 1
		private var textEncodings: TextEncoding? = null

		/**
		 * Get singleton for this class.
		 *
		 * @return singleton
		 */
		@JvmStatic
		@get:Synchronized
		val instanceOf: TextEncoding
			get() {
				if (textEncodings == null) {
					textEncodings = TextEncoding()
				}
				return textEncodings!!
			}
	}
}
