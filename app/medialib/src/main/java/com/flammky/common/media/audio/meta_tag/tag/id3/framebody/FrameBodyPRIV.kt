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
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.StringNullTerminated
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * Private frame.
 *
 *
 * This frame is used to contain information from a software producer
 * that its program uses and does not fit into the other frames. The
 * frame consists of an 'Owner identifier' string and the binary data.
 * The 'Owner identifier' is a null-terminated string with a URL
 * containing an email address, or a link to a location where an email
 * address can be found, that belongs to the organisation responsible
 * for the frame. Questions regarding the frame should be sent to the
 * indicated email address. The tag may contain more than one "PRIV"
 * frame but only with different contents. It is recommended to keep the
 * number of "PRIV" frames as low as possible.
 *
 * Header for 'Private frame'
 * Owner identifier
 * The private data
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
class FrameBodyPRIV : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_PRIVATE

	/**
	 * Creates a new FrameBodyPRIV datatype.
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_OWNER, "")
		setObjectValue(DataTypes.OBJ_DATA, ByteArray(0))
	}

	constructor(body: FrameBodyPRIV) : super(body)

	/**
	 * Creates a new FrameBodyPRIV datatype.
	 *
	 * @param owner
	 * @param data
	 */
	constructor(owner: String?, data: ByteArray?) {
		setObjectValue(DataTypes.OBJ_OWNER, owner)
		setObjectValue(DataTypes.OBJ_DATA, data)
	}

	/**
	 * Creates a new FrameBodyPRIV datatype.
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
	 * @param data
	 */
	var data: ByteArray?
		get() = getObjectValue(DataTypes.OBJ_DATA) as ByteArray
		set(data) {
			setObjectValue(DataTypes.OBJ_DATA, data)
		}
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
		objectList.add(ByteArraySizeTerminated(DataTypes.OBJ_DATA, this))
	}
}
