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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberHashMap
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.TextEncodedStringSizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getUnicodeTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Abstract representation of a Text Frame
 *
 * The text information frames are often the most important frames, containing information like artist, album and
 * more. There may only be  one text information frame of its kind in an tag. In ID3v24 All text information frames
 * supports multiple strings, stored as a null separated list, where null is represented by the termination code
 * for the character encoding. All text frame identifiers begin with "T". Only text frame identifiers begin with "T",
 * with the exception of the "TXXX" frame. All the text information frames have the following  format:
 *
 * Header for 'Text information frame', ID: "T000" - "TZZZ",
 * excluding "TXXX" described in 4.2.6.
 *
 * Text encoding                $xx
 * Information                  text string(s) according to encoding
 *
 * The list of valid text encodings increased from two in ID3v23 to four in ID3v24
 *
 * iTunes incorrectly writes null terminators at the end of every String, even though it only writes one String.
 *
 * You can retrieve the first value without the null terminator using [.getFirstTextValue]
 */
abstract class AbstractFrameBodyTextInfo : AbstractID3v2FrameBody {
	/**
	 * Creates a new FrameBodyTextInformation datatype. The super.super
	 * Constructor sets up the Object list for the frame.
	 */
	protected constructor() : super() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, "")
	}

	/**
	 * Copy Constructor
	 *
	 * @param body AbstractFrameBodyTextInformation
	 */
	protected constructor(body: AbstractFrameBodyTextInfo) : super(body)

	/**
	 * Creates a new FrameBodyTextInformation data type. This is used when user
	 * wants to create a new frame based on data in a user interface.
	 *
	 * @param textEncoding Specifies what encoding should be used to write
	 * text to file.
	 * @param text         Specifies the text String.
	 */
	protected constructor(textEncoding: Byte, text: String?) : super() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_TEXT, text)
	}

	/**
	 * Creates a new FrameBodyTextInformation data type from file.
	 *
	 *
	 * The super.super Constructor sets up the Object list for the frame.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	protected constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	override val userFriendlyValue: String
		get() = textWithoutTrailingNulls
	/**
	 * Retrieve the complete text String as it is held internally.
	 *
	 * If multiple values are held these will be returned, needless trailing nulls will also be returned
	 *
	 * @return the text string
	 */
	/**
	 * Set the Full Text String.
	 *
	 *
	 * If this String contains null terminator characters these are parsed as value
	 * separators, allowing you to hold multiple strings within one text frame. This functionality is only
	 * officially support in ID3v24.
	 *
	 * @param text to set
	 */
	var text: String?
		get() = getObjectValue(DataTypes.OBJ_TEXT) as String
		set(text) {
			requireNotNull(text) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			setObjectValue(DataTypes.OBJ_TEXT, text)
		}

	/**
	 * Retrieve the complete text String but without any trailing nulls
	 *
	 * If multiple values are held these will be returned, needless trailing nulls will not be returned
	 *
	 * @return the text string
	 */
	val textWithoutTrailingNulls: String
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
			return text!!.valueWithoutTrailingNull
		}

	/**
	 * Get first value
	 *
	 * @return value at index 0
	 */
	val firstTextValue: String
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
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
	fun getValueAtIndex(index: Int): String {
		val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
		return text!!.getValueAtIndex(index)
	}

	val values: List<String>
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
			return text!!.values
		}

	/**
	 * Add additional value to value
	 *
	 * @param value at index
	 */
	fun addTextValue(value: String) {
		val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
		text?.addValue(value)
	}

	/**
	 * @return number of text values, usually one
	 */
	val numberOfValues: Int
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
			return text!!.numberOfValues
		}

	/**
	 * Because Text frames have a text encoding we need to check the text
	 * String does not contain characters that cannot be encoded in
	 * current encoding before we write data. If there are change the text
	 * encoding.
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		//Ensure valid for type
		textEncoding = getTextEncoding(header, textEncoding)

		//Ensure valid for data
		if (!(getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		super.write(tagBuffer)
	}

	/**
	 * Setup the Object List. All text frames contain a text encoding
	 * and then a text string.
	 *
	 * TODO:would like to make final but cannot because overridden by FrameBodyTXXX
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(TextEncodedStringSizeTerminated(DataTypes.OBJ_TEXT, this))
	}
}
