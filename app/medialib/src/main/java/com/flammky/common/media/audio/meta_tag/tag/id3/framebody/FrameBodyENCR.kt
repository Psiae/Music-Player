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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.ByteArraySizeTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberFixedLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.StringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * Encryption method registration frame.
 *
 *
 * To identify with which method a frame has been encrypted the
 * encryption method must be registered in the tag with this frame. The
 * 'Owner identifier' is a null-terminated string with a URL
 * containing an email address, or a link to a location where an email
 * address can be found, that belongs to the organisation responsible
 * for this specific encryption method. Questions regarding the
 * encryption method should be sent to the indicated email address. The
 * 'Method symbol' contains a value that is associated with this method
 * throughout the whole tag. Values below $80 are reserved. The 'Method
 * symbol' may optionally be followed by encryption specific data. There
 * may be several "ENCR" frames in a tag but only one containing the
 * same symbol and only one containing the same owner identifier. The
 * method must be used somewhere in the tag. See section 3.3.1, flag j
 * for more information.
 *
 * <table border=0 width="70%">
 * <tr><td colspan=2>&lt;Header for 'Encryption method registration', ID: "ENCR"&gt;</td></tr>
 * <tr><td>Owner identifier</td><td width="80%">&lt;text string&gt; $00</td></tr>
 * <tr><td>Method symbol   </td><td>$xx                           </td></tr>
 * <tr><td>Encryption data </td><td>&lt;binary data&gt;           </td></tr>
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
class FrameBodyENCR : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {


	/** [org.jaudiotagger.tag.id3.framebody.FrameBodyENCR] */

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_ENCRYPTION

	/**
	 * Creates a new FrameBodyENCR datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_OWNER, "")
		setObjectValue(DataTypes.OBJ_METHOD_SYMBOL, 0.toByte())
		setObjectValue(DataTypes.OBJ_ENCRYPTION_INFO, ByteArray(0))
	}

	constructor(body: FrameBodyENCR) : super(body)

	/**
	 * Creates a new FrameBodyENCR datatype.
	 *
	 * @param owner
	 * @param methodSymbol
	 * @param data
	 */
	constructor(owner: String?, methodSymbol: Byte, data: ByteArray?) {
		setObjectValue(DataTypes.OBJ_OWNER, owner)
		setObjectValue(DataTypes.OBJ_METHOD_SYMBOL, methodSymbol)
		setObjectValue(DataTypes.OBJ_ENCRYPTION_INFO, data)
	}

	/**
	 * Creates a new FrameBodyENCR datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
	/**
	 * @return
	 */
	/**
	 * @param owner
	 */
	var owner: String?
		get() = getObjectValue(DataTypes.OBJ_OWNER) as String
		set(owner) {
			setObjectValue(DataTypes.OBJ_OWNER, owner)
		}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(StringNullTerminated(DataTypes.OBJ_OWNER, this))
		objectList.add(NumberFixedLength(DataTypes.OBJ_METHOD_SYMBOL, this, 1))
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_ENCRYPTION_INFO, this))
	}
}
