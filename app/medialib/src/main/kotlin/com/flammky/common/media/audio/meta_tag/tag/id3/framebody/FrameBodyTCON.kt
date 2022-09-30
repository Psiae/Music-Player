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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.TCONString
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ID3V2ExtendedGenreTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.GenreTypes
import java.nio.ByteBuffer

/**
 * Content type Text information frame.
 *
 *
 * The 'Content type', which previously was
 * stored as a one byte numeric value only, is now a numeric string. You
 * may use one or several of the types as ID3v1.1 did or, since the
 * category list would be impossible to maintain with accurate and up to
 * date categories, define your own.
 *
 *
 * ID3V23:References to the ID3v1 genres can be made by, as first byte, enter
 * "(" followed by a number from the genres list (appendix A) and
 * ended with a ")" character. This is optionally followed by a
 * refinement, e.g. "(21)" or "(4)Eurodisco". Several references can be
 * made in the same frame, e.g. "(51)(39)". If the refinement should
 * begin with a "(" character it should be replaced with "((", e.g. "((I
 * can figure out any genre)" or "(55)((I think...)". The following new
 * content types is defined in ID3v2 and is implemented in the same way
 * as the numeric content types, e.g. "(RX)".
 *
 * <table border=0 width="70%">
 * <tr><td>RX</td><td width="100%">Remix</td></tr>
 * <tr><td>CR</td><td>Cover</td></tr>
</table> *
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * ID3V24:The 'Content type', which ID3v1 was stored as a one byte numeric
 * value only, is now a string. You may use one or several of the ID3v1
 * types as numerical strings, or, since the category list would be
 * impossible to maintain with accurate and up to date categories,
 * define your own. Example: "21" $00 "Eurodisco" $00
 *
 * You may also use any of the following keywords:
 *
 * <table border=0 width="70%">
 * <tr><td>RX</td><td width="100%">Remix</td></tr>
 * <tr><td>CR</td><td>Cover</td></tr>
</table> *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyTCON : AbstractFrameBodyTextInfo, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_GENRE

	/**
	 * Creates a new FrameBodyTCON datatype.
	 */
	constructor()
	constructor(body: FrameBodyTCON) : super(body)

	/**
	 * Creates a new FrameBodyTCON datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTCON datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	fun setV23Format() {
		val text = getObject(DataTypes.OBJ_TEXT) as TCONString?
		text!!.isNullSeperateMultipleValues = false
	}

	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(TCONString(DataTypes.OBJ_TEXT, this))
	}

	companion object {
		/**
		 * Convert value to internal genre value
		 *
		 * @param value
		 * @return
		 */
		fun convertGenericToID3v24Genre(value: String): String {
			var value = value
			try {
				//If passed id and known value use it
				val genreId = value.toInt()
				return if (genreId <= GenreTypes.maxGenreId) {
					genreId.toString()
				} else {
					value
				}
			} catch (nfe: NumberFormatException) {
				// If passed String, use matching integral value if can
				val genreId = GenreTypes.instanceOf.getIdForName(value)
				// to preserve iTunes compatibility, don't write genre ids higher than getMaxStandardGenreId, rather use string
				if (genreId != null && genreId <= GenreTypes.maxStandardGenreId) {
					return genreId.toString()
				}

				//Covert special string values
				if (value.equals(ID3V2ExtendedGenreTypes.RX.description, ignoreCase = true)) {
					value = ID3V2ExtendedGenreTypes.RX.name
				} else if (value.equals(
						ID3V2ExtendedGenreTypes.CR.description,
						ignoreCase = true
					)
				) {
					value = ID3V2ExtendedGenreTypes.CR.name
				} else if (value.equals(ID3V2ExtendedGenreTypes.RX.name, ignoreCase = true)) {
					value = ID3V2ExtendedGenreTypes.RX.name
				} else if (value.equals(ID3V2ExtendedGenreTypes.CR.name, ignoreCase = true)) {
					value = ID3V2ExtendedGenreTypes.CR.name
				}
			}
			return value
		}

		/**
		 * Convert value to internal genre value
		 *
		 * @param value
		 * @return
		 */
		fun convertGenericToID3v23Genre(value: String): String {
			var value = value
			try {
				//If passed integer and in list use numeric form else use original value
				val genreId = value.toInt()
				return if (genreId <= GenreTypes.maxGenreId) {
					bracketWrap(genreId.toString())
				} else {
					value
				}
			} catch (nfe: NumberFormatException) {
				//if passed text try and find integral value otherwise use text
				val genreId = GenreTypes.instanceOf.getIdForName(value)
				// to preserve iTunes compatibility, don't write genre ids higher than getMaxStandardGenreId, rather use string
				if (genreId != null && genreId <= GenreTypes.maxStandardGenreId) {
					return bracketWrap(genreId.toString())
				}

				//But special handling for these text values
				if (value.equals(ID3V2ExtendedGenreTypes.RX.description, ignoreCase = true)) {
					value = bracketWrap(ID3V2ExtendedGenreTypes.RX.name)
				} else if (value.equals(
						ID3V2ExtendedGenreTypes.CR.description,
						ignoreCase = true
					)
				) {
					value = bracketWrap(ID3V2ExtendedGenreTypes.CR.name)
				} else if (value.equals(ID3V2ExtendedGenreTypes.RX.name, ignoreCase = true)) {
					value = bracketWrap(ID3V2ExtendedGenreTypes.RX.name)
				} else if (value.equals(ID3V2ExtendedGenreTypes.CR.name, ignoreCase = true)) {
					value = bracketWrap(ID3V2ExtendedGenreTypes.CR.name)
				}
			}
			return value
		}

		fun convertGenericToID3v22Genre(value: String): String {
			return convertGenericToID3v23Genre(value)
		}

		private fun bracketWrap(value: Any): String {
			return "($value)"
		}

		/**
		 * Convert internal v24 genre value to generic genre
		 *
		 * @param value
		 * @return
		 */
		fun convertID3v24GenreToGeneric(value: String): String? {
			var value = value
			value = try {
				val genreId = value.toInt()
				return if (genreId <= GenreTypes.maxGenreId) {
					GenreTypes.instanceOf.getValueForId(genreId)
				} else {
					value
				}
			} catch (nfe: NumberFormatException) {
				if (value.equals(ID3V2ExtendedGenreTypes.RX.name, ignoreCase = true)) {
					ID3V2ExtendedGenreTypes.RX.description
				} else if (value.equals(ID3V2ExtendedGenreTypes.CR.name, ignoreCase = true)) {
					ID3V2ExtendedGenreTypes.CR.description
				} else {
					return value
				}
			}
			return value
		}

		private fun checkBracketed(value: String): String? {
			var value = value
			value = value.replace("(", "")
			value = value.replace(")", "")
			value = try {
				val genreId = value.toInt()
				return if (genreId <= GenreTypes.maxGenreId) {
					GenreTypes.instanceOf.getValueForId(genreId)
				} else {
					value
				}
			} catch (nfe: NumberFormatException) {
				if (value.equals(ID3V2ExtendedGenreTypes.RX.name, ignoreCase = true)) {
					ID3V2ExtendedGenreTypes.RX.description
				} else if (value.equals(ID3V2ExtendedGenreTypes.CR.name, ignoreCase = true)) {
					ID3V2ExtendedGenreTypes.CR.description
				} else {
					return value
				}
			}
			return value
		}

		/**
		 * Convert V23 format to Generic
		 *
		 * i.e.
		 *
		 * (2)         -> Country
		 * (RX)        -> Remix
		 * Shoegaze    -> Shoegaze
		 * (2)Shoegaze -> Country Shoegaze
		 *
		 * Note only handles one field so if the frame stored (2)(3) this would be two separate fields
		 * and would manifest itself as two different calls to this method once for (2) and once for (3)
		 * @param value
		 * @return
		 */
		fun convertID3v23GenreToGeneric(value: String): String? {
			return if (value.contains(")") && value.lastIndexOf(')') < value.length - 1) {
				checkBracketed(
					value.substring(0, value.lastIndexOf(')'))
				) + ' ' + value.substring(value.lastIndexOf(')') + 1)
			} else {
				checkBracketed(
					value
				)
			}
		}

		fun convertID3v22GenreToGeneric(value: String): String? {
			return convertID3v23GenreToGeneric(value)
		}
	}
}
