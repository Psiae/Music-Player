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
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * General encapsulated object frame.
 *
 *
 * In this frame any type of file can be encapsulated. After the header,
 * 'Frame size' and 'Encoding' follows 'MIME type' represented as
 * as a terminated string encoded with ISO-8859-1. The
 * filename is case sensitive and is encoded as 'Encoding'. Then follows
 * a content description as terminated string, encoded as 'Encoding'.
 * The last thing in the frame is the actual object. The first two
 * strings may be omitted, leaving only their terminations. There may be more than one "GEOB"
 * frame in each tag, but only one with the same content descriptor.
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2> &lt;Header for 'General encapsulated object', ID: "GEOB"&gt;</td></tr>
 * <tr><td>Text encoding       </td><td>$xx                     </td></tr>
 * <tr><td>MIME type           </td><td>&lt;text string&gt; $00 </td></tr>
 * <tr><td>Filename            </td><td>&lt;text string according to encoding&gt; $00 (00)</td></tr>
 * <tr><td>Content description </td><td><text string according to encoding> $00 (00)</text></td></tr>
 * <tr><td>Encapsulated object </td><td>&lt;binary data&gt;     </td></tr>
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
class FrameBodyGEOB : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_GENERAL_ENCAPS_OBJECT

	/**
	 * Creates a new FrameBodyGEOB datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_MIME_TYPE, "")
		setObjectValue(DataTypes.OBJ_FILENAME, "")
		setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		setObjectValue(DataTypes.OBJ_DATA, ByteArray(0))
	}

	constructor(body: FrameBodyGEOB) : super(body)

	/**
	 * Creates a new FrameBodyGEOB datatype.
	 *
	 * @param textEncoding
	 * @param mimeType
	 * @param filename
	 * @param description
	 * @param object
	 */
	constructor(
		textEncoding: Byte,
		mimeType: String?,
		filename: String?,
		description: String?,
		`object`: ByteArray?
	) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_MIME_TYPE, mimeType)
		setObjectValue(DataTypes.OBJ_FILENAME, filename)
		setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		setObjectValue(DataTypes.OBJ_DATA, `object`)
	}

	/**
	 * Creates a new FrameBodyGEOB datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return the description field
	 */
	/**
	 * @param description
	 */
	var description: String?
		get() = getObjectValue(DataTypes.OBJ_DESCRIPTION) as String
		set(description) {
			setObjectValue(DataTypes.OBJ_DESCRIPTION, description)
		}

	/**
	 * If the filename or description cannot be encoded using current encoder, change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		if (!(getObject(DataTypes.OBJ_FILENAME) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = TextEncoding.UTF_16
		}
		if (!(getObject(DataTypes.OBJ_DESCRIPTION) as AbstractString?)!!.canBeEncoded()) {
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
		objectList.add(StringNullTerminated(DataTypes.OBJ_MIME_TYPE, this))
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_FILENAME, this))
		objectList.add(TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_DATA, this))
	}
}
