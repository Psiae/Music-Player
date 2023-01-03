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

import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getUnicodeTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Comments frame.
 *
 *
 * This frame is intended for any kind of full text information that does not fit in any other frame. It consists of a
 * frame header followed by encoding, language and content descriptors and is ended with the actual comment as a
 * text string. Newline characters are allowed in the comment text string. There may be more than one comment frame
 * in each tag, but only one with the same language and* content descriptor.
 *
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2>&lt;Header for 'Comment', ID: "COMM"&gt;</td></tr>
 * <tr><td>Text encoding   </td><td width="80%">$xx          </td></tr>
 * <tr><td>Language        </td><td>$xx xx xx                </td></tr>
 * <tr><td>Short content descrip.</td><td>&lt;text string according to encoding&gt; $00 (00)</td></tr>
 * <tr><td>The actual text </td><td>&lt;full text string according to encoding&gt;</td></tr>
</table> *
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyCOMM : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_COMMENT

	val isMediaMonkeyFrame: Boolean
		get() {
			val desc = description
			if (desc != null && desc.length != 0) {
				if (desc.startsWith(MM_PREFIX)) {
					return true
				}
			}
			return false
		}
	val isItunesFrame: Boolean
		get() {
			val desc = description
			if (desc != null && desc.length != 0) {
				if (desc == ITUNES_NORMALIZATION) {
					return true
				}
			}
			return false
		}

	/**
	 * Creates a new FrameBodyCOMM datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_LANGUAGE, Languages.DEFAULT_ID)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		setObjectValue(DataTypes.OBJ_TEXT, "")
	}

	constructor(body: FrameBodyCOMM) : super(body)

	/**
	 * Creates a new FrameBodyCOMM datatype.
	 *
	 * @param textEncoding
	 * @param language
	 * @param description
	 * @param text
	 */
	constructor(textEncoding: Byte, language: String?, description: String?, text: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_LANGUAGE, language)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		setObjectValue(DataTypes.OBJ_TEXT, text)
	}

	/**
	 * Construct a Comment frame body from the buffer
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * Get the description field, which describes the type of comment
	 *
	 * @return description field
	 */
	/**
	 * Set the description field, which describes the type of comment
	 *
	 * @param description
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			requireNotNull(description) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}
	/**
	 * Get the language the comment is written in
	 *
	 * @return the language
	 *///TODO not sure if this might break existing code
	/*if(language==null)
	 {
				throw new IllegalArgumentException(ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.getMsg());
	 } */
	/**
	 * Sets the language the comment is written in
	 *
	 * @param language
	 */
	var language: String?
		get() = getObjectValue(DataTypes.OBJ_LANGUAGE) as String
		set(language) {
			//TODO not sure if this might break existing code
			/*if(language==null)
	{
			 throw new IllegalArgumentException(ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.getMsg());
	} */
			setObjectValue(DataTypes.OBJ_LANGUAGE, language)
		}
	/**
	 * Returns the the text field which holds the comment, adjusted to ensure does not return trailing null
	 * which is due to a iTunes bug.
	 *
	 * @return the text field
	 */
	/**
	 * @param text
	 */
	var text: String?
		get() {
			val text = getObject(DataTypes.OBJ_TEXT) as TextEncodedStringSizeTerminated?
			return text!!.getValueAtIndex(0)
		}
		set(text) {
			requireNotNull(text) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
			setObjectValue(DataTypes.OBJ_TEXT, text)
		}
	override val userFriendlyValue: String
		get() = text!!

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(StringHashMap(DataTypes.OBJ_LANGUAGE, this, Languages.LANGUAGE_FIELD_SIZE))
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(TextEncodedStringSizeTerminated(DataTypes.OBJ_TEXT, this))
	}

	/**
	 * Because COMM have a text encoding we need to check the text String does
	 * not contain characters that cannot be encoded in current encoding before
	 * we write data. If there are we change the encoding.
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		//Ensure valid for type
		textEncoding = getTextEncoding(header, textEncoding)

		//Ensure valid for data
		if (!(getObject(DataTypes.OBJ_TEXT) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		super.write(tagBuffer)
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

	companion object {
		//Most players only read comment with description of blank
		const val DEFAULT = ""

		//used by iTunes for volume normalization, although uses the COMMENT field not usually displayed as a comment
		const val ITUNES_NORMALIZATION = "iTunNORM"

		//Various descriptions used by MediaMonkey, (note Media Monkey uses non-standard language field XXX)
		private const val MM_PREFIX = "Songs-DB"
		const val MM_CUSTOM1 = "Songs-DB_Custom1"
		const val MM_CUSTOM2 = "Songs-DB_Custom2"
		const val MM_CUSTOM3 = "Songs-DB_Custom3"
		const val MM_CUSTOM4 = "Songs-DB_Custom4"
		const val MM_CUSTOM5 = "Songs-DB_Custom5"
		const val MM_OCCASION = "Songs-DB_Occasion"
		const val MM_QUALITY = "Songs-DB_Preference"
		const val MM_TEMPO = "Songs-DB_Tempo"
	}
}
