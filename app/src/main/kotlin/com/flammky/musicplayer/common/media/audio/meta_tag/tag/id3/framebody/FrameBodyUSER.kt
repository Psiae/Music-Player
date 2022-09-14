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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Terms of use frame.
 *
 *
 * This frame contains a brief description of the terms of use and
 * ownership of the file. More detailed information concerning the legal
 * terms might be available through the "WCOP" frame. Newlines are
 * allowed in the text. There may only be one "USER" frame in a tag.
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2>&lt;Header for 'Terms of use frame', ID: "USER"&gt;</td></tr>
 * <tr><td>Text encoding  </td><td>$xx</td></tr>
 * <tr><td>Language       </td><td>$xx xx xx</td></tr>
 * <tr><td>The actual text</td><td>&lt;text string according to encoding&gt;</td></tr>
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
class FrameBodyUSER : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_TERMS_OF_USE

	/**
	 * Creates a new FrameBodyUSER datatype.
	 */
	constructor() {
		//        setObject("Text Encoding", new Byte((byte) 0));
		//        setObject("Language", "");
		//        setObject("Text", "");
	}

	constructor(body: FrameBodyUSER) : super(body)

	/**
	 * Creates a new FrameBodyUSER datatype.
	 *
	 * @param textEncoding
	 * @param language
	 * @param text
	 */
	constructor(textEncoding: Byte, language: String?, text: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_LANGUAGE, language)
		setObjectValue(DataTypes.OBJ_TEXT, text)
	}

	/**
	 * Create a new FrameBodyUser by reading from byte buffer
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * @return lanaguage
	 */
	val language: String
		get() = getObjectValue(DataTypes.OBJ_LANGUAGE) as String

	/**
	 * @param language
	 */
	fun setOwner(language: String?) {
		setObjectValue(DataTypes.OBJ_LANGUAGE, language)
	}

	/**
	 * If the text cannot be encoded using current encoder, change the encoder
	 *
	 * @param tagBuffer
	 * @throws java.io.IOException
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (!(getObject(DataTypes.OBJ_TEXT) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = TextEncoding.UTF_16
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
		objectList.add(StringSizeTerminated(DataTypes.OBJ_TEXT, this))
	}
}
