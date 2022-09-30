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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.lyrics3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.AbstractStringStringValuePair

class Lyrics3v2Fields private constructor() : AbstractStringStringValuePair() {


	init {
		idToValue[FIELD_V2_INDICATIONS] = "Indications field"
		idToValue[FIELD_V2_LYRICS_MULTI_LINE_TEXT] = "Lyrics multi line text"
		idToValue[FIELD_V2_ADDITIONAL_MULTI_LINE_TEXT] = "Additional information multi line text"
		idToValue[FIELD_V2_AUTHOR] = "Lyrics/Music Author name"
		idToValue[FIELD_V2_ALBUM] = "Extended Album name"
		idToValue[FIELD_V2_ARTIST] = "Extended Artist name"
		idToValue[FIELD_V2_TRACK] = "Extended Track Title"
		idToValue[FIELD_V2_IMAGE] = "Link to an image files"
		createMaps()
	}

	companion object {
		private var lyrics3Fields: Lyrics3v2Fields? = null

		/**
		 * CRLF int set
		 */
		private val crlfByte = byteArrayOf(13, 10)

		/**
		 * CRLF int set
		 */
		val CRLF = String(crlfByte)
		val instanceOf: Lyrics3v2Fields
			get() {
				if (lyrics3Fields == null) {
					lyrics3Fields = Lyrics3v2Fields()
				}
				return lyrics3Fields!!
			}
		const val FIELD_V2_INDICATIONS = "IND"
		const val FIELD_V2_LYRICS_MULTI_LINE_TEXT = "LYR"
		const val FIELD_V2_ADDITIONAL_MULTI_LINE_TEXT = "INF"
		const val FIELD_V2_AUTHOR = "AUT"
		const val FIELD_V2_ALBUM = "EAL"
		const val FIELD_V2_ARTIST = "EAR"
		const val FIELD_V2_TRACK = "ETT"
		const val FIELD_V2_IMAGE = "IMG"

		/**
		 * Returns true if the identifier is a valid Lyrics3v2 frame identifier
		 *
		 * @param identifier string to test
		 * @return true if the identifier is a valid Lyrics3v2 frame identifier
		 */
		fun isLyrics3v2FieldIdentifier(identifier: String): Boolean {
			return identifier.length >= 3 && instanceOf.getIdToValueMap()
				.containsKey(identifier.substring(0, 3))
		}
	}
}
