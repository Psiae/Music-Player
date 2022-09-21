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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getUnicodeTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Unsychronised lyrics/text transcription frame.
 *
 *
 * This frame contains the lyrics of the song or a text transcription of other vocal activities. The head includes an
 * encoding descriptor and a content descriptor. The body consists of the actual text. The 'Content descriptor' is a
 * terminated string. If no descriptor is entered, 'Content descriptor' is $00 (00) only. Newline characters are
 * allowed in the text. There may be more than one 'Unsynchronised lyrics/text transcription' frame in each tag, but
 * only one with the same language and content descriptor.
 *
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2>&lt;Header for 'Unsynchronised lyrics/text transcription', ID: "USLT"&gt;</td></tr>
 * <tr><td>Text encoding     </td><td width="80%">$xx</td></tr>
 * <tr><td>Language          </td><td>$xx xx xx</td></tr>
 * <tr><td>Content descriptor</td><td>&lt;text string according to encoding&gt; $00 (00)</td></tr>
 * <tr><td>Lyrics/text       </td><td>&lt;full text string according to encoding&gt;</td></tr>
</table> *
 *
 * You can retrieve the first value without the null terminator using [.getFirstTextValue]
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
class FrameBodyUSLT : AbstractID3v2FrameBody, ID3v23FrameBody, ID3v24FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_UNSYNC_LYRICS

	/**
	 * Creates a new FrameBodyUSLT dataType.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_LANGUAGE, "")
		setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		setObjectValue(DataTypes.OBJ_LYRICS, "")
	}

	/**
	 * Copy constructor
	 *
	 * @param body
	 */
	constructor(body: FrameBodyUSLT) : super(body)

	/**
	 * Creates a new FrameBodyUSLT datatype.
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
		setObjectValue(DataTypes.OBJ_LYRICS, text)
	}

	/**
	 * Creates a new FrameBodyUSLT datatype, populated from buffer
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	override val userFriendlyValue: String
		get() = firstTextValue
	/**
	 * Get a description field
	 *
	 * @return description
	 */
	/**
	 * Set a description field
	 *
	 * @param description
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}
	/**
	 * Get the language field
	 *
	 * @return language
	 */
	/**
	 * Set the language field
	 *
	 * @param language
	 */
	var language: String?
		get() = getObjectValue(DataTypes.OBJ_LANGUAGE) as String
		set(language) {
			setObjectValue(DataTypes.OBJ_LANGUAGE, language)
		}
	/**
	 * Get the lyric field
	 *
	 * @return lyrics
	 */
	/**
	 * Set the lyric field
	 *
	 * @param lyric
	 */
	var lyric: String?
		get() = getObjectValue(DataTypes.OBJ_LYRICS) as String
		set(lyric) {
			setObjectValue(DataTypes.OBJ_LYRICS, lyric)
		}

	/**
	 * Get first value
	 *
	 * @return value at index 0
	 */
	val firstTextValue: String
		get() {
			val text = getObject(DataTypes.OBJ_LYRICS) as TextEncodedStringSizeTerminated?
			return text!!.getValueAtIndex(0)
		}

	/**
	 * Add additional lyric to the lyric field
	 *
	 * @param text
	 */
	fun addLyric(text: String) {
		lyric = lyric + text
	}

	/**
	 * @param line
	 */
	fun addLyric(line: Lyrics3Line) {
		lyric = lyric + line.writeString()
	}

	override fun write(tagBuffer: ByteArrayOutputStream) {

		//Ensure valid for type
		textEncoding = getTextEncoding(header, textEncoding)

		//Ensure valid for data
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		if (!(getObject(DataTypes.OBJ_LYRICS) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		super.write(tagBuffer)
	}

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
		objectList.add(TextEncodedStringSizeTerminated(DataTypes.OBJ_LYRICS, this))
	}
}
