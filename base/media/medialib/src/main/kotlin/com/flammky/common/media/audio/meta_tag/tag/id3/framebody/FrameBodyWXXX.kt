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
 * Represents a user defined URL,must also privide a description
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Represents a user defined url
 */
class FrameBodyWXXX : AbstractFrameBodyUrlLink, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_USER_DEFINED_URL

	/**
	 * Creates a new FrameBodyWXXX datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		setObjectValue(DataTypes.OBJ_URLLINK, "")
	}

	constructor(body: FrameBodyWXXX) : super(body)

	/**
	 * Creates a new FrameBodyWXXX datatype.
	 *
	 * @param textEncoding
	 * @param description
	 * @param urlLink
	 */
	constructor(textEncoding: Byte, description: String?, urlLink: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		setObjectValue(DataTypes.OBJ_URLLINK, urlLink)
	}

	/**
	 * Creates a new FrameBodyWXXX datatype by reading from file.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return a description of the hyperlink
	 */
	/**
	 * Set a description of the hyperlink
	 *
	 * @param description
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}

	/**
	 * If the description cannot be encoded using the current encoding change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = TextEncoding.UTF_16
		}
		super.write(tagBuffer)
	}

	/**
	 * This is different ot other URL Links
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(StringSizeTerminated(DataTypes.OBJ_URLLINK, this))
	}

	/**
	 * Retrieve the complete text String but without any trailing nulls
	 *
	 * If multiple values are held these will be returned, needless trailing nulls will not be returned
	 *
	 * @return the text string
	 */
	val urlLinkWithoutTrailingNulls: String
		get() {
			val text = getObject(DataTypes.OBJ_URLLINK) as TextEncodedStringSizeTerminated?
			return text!!.valueWithoutTrailingNull
		}

	/**
	 * Get first value
	 *
	 * @return value at index 0
	 */
	val firstUrlLink: String
		get() {
			val text = getObject(DataTypes.OBJ_URLLINK) as TextEncodedStringSizeTerminated?
			return text!!.getValueAtIndex(0)
		}

	/**
	 * Get text value at index
	 *
	 * When a multiple values are stored within a single text frame this method allows access to any of the
	 * individual values.
	 *
	 * @param index
	 * @return value at index
	 */
	fun getUrlLinkAtIndex(index: Int): String {
		val text = getObject(DataTypes.OBJ_URLLINK) as TextEncodedStringSizeTerminated?
		return text!!.getValueAtIndex(index)
	}

	val urlLinks: List<String>
		get() {
			val text = getObject(DataTypes.OBJ_URLLINK) as TextEncodedStringSizeTerminated?
			return text!!.values
		}

	/**
	 * Add additional value to value
	 *
	 * @param value at index
	 */
	fun addUrlLink(value: String) {
		val text = getObject(DataTypes.OBJ_URLLINK) as TextEncodedStringSizeTerminated?
		text?.addValue(value)
	}

	companion object {
		const val URL_DISCOGS_RELEASE_SITE = "DISCOGS_RELEASE"
		const val URL_WIKIPEDIA_RELEASE_SITE = "WIKIPEDIA_RELEASE"
		const val URL_OFFICIAL_RELEASE_SITE = "OFFICIAL_RELEASE"
		const val URL_DISCOGS_ARTIST_SITE = "DISCOGS_ARTIST"
		const val URL_WIKIPEDIA_ARTIST_SITE = "WIKIPEDIA_ARTIST"
		const val URL_LYRICS_SITE = "LYRICS_SITE"
	}
}
